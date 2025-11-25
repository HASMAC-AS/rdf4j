/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.lmdb.lftj.IndexSelector;
import org.eclipse.rdf4j.sail.lmdb.lftj.LFTJExecutor;
import org.eclipse.rdf4j.sail.lmdb.lftj.LMDBTrieIterator;
import org.eclipse.rdf4j.sail.lmdb.lftj.LeapfrogIteratorCursor;
import org.eclipse.rdf4j.sail.lmdb.lftj.LmdbWCOJ;
import org.eclipse.rdf4j.sail.lmdb.lftj.Prefix;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyOrder;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadPattern;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadPatternTerm;
import org.eclipse.rdf4j.sail.lmdb.lftj.Slot;
import org.eclipse.rdf4j.sail.lmdb.lftj.TrieIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query evaluation step that executes an {@link LmdbWCOJ} using {@link LFTJExecutor}.
 */
public class LmdbWCOJStep implements QueryEvaluationStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(LmdbWCOJStep.class);

	private final LmdbWCOJ wcoj;
	private final List<LmdbDatasetSnapshot> snapshots;
	private final QueryEvaluationContext context;
	private final Function<LmdbWCOJ, TupleExpr> rebuildJoin;
	private final EvaluationStrategy strategy;
	private final Supplier<MutableBindingSet> bindingSetSupplier;
	private final Function<String, BiConsumer<Value, MutableBindingSet>> bindingSetterFactory;

	public LmdbWCOJStep(LmdbWCOJ wcoj, List<LmdbDatasetSnapshot> snapshots, QueryEvaluationContext context,
			Function<LmdbWCOJ, TupleExpr> rebuildJoin,
			EvaluationStrategy strategy) {
		this.wcoj = wcoj;
		this.snapshots = List.copyOf(Objects.requireNonNull(snapshots, "snapshots"));
		this.context = context;
		this.rebuildJoin = rebuildJoin;
		this.strategy = strategy;
		this.bindingSetSupplier = context::createBindingSet;
		this.bindingSetterFactory = context::setBinding;
	}

	@Override
	public CloseableIteratorIteration<BindingSet> evaluate(BindingSet bindings) throws QueryEvaluationException {
		if (bindings != null && !bindings.isEmpty()) {
			// fall back to the standard join pipeline for bound input
			TupleExpr delegate = rebuildJoin.apply(wcoj);
			QueryEvaluationStep fallback = strategy.precompile(delegate, context);
			return new CloseableIteratorIteration<>(fallback.evaluate(bindings));
		}

		AtomicBoolean cancelled = new AtomicBoolean(false);

		BindingIterator iterator = new BindingIterator(cancelled);
		return new SingleThreadWCOJIteration(iterator, cancelled);
	}

	private MutableBindingSet toBindingSet(List<String> variableOrder, long[] values, boolean[] present,
			ValueStoreFacade valueStore) throws QueryEvaluationException {
		MutableBindingSet bs = bindingSetSupplier.get();
		for (int i = 0; i < variableOrder.size(); i++) {
			if (!present[i]) {
				continue;
			}
			try {
				String variable = variableOrder.get(i);
				BiConsumer<Value, MutableBindingSet> setter = bindingSetterFactory.apply(variable);
				setter.accept(valueStore.getValue(values[i]), bs);
			} catch (IOException e) {
				throw new QueryEvaluationException(e);
			}
		}
		return bs;
	}

	private List<QuadPattern> toQuadPatterns(List<StatementPattern> patterns, ValueStoreFacade valueStore)
			throws QueryEvaluationException {
		List<QuadPattern> quadPatterns = new ArrayList<>(patterns.size());
		for (StatementPattern pattern : patterns) {
			quadPatterns.add(QuadPattern.of(
					toTerm(pattern.getSubjectVar(), valueStore),
					toTerm(pattern.getPredicateVar(), valueStore),
					toTerm(pattern.getObjectVar(), valueStore),
					toTerm(pattern.getContextVar(), valueStore)));
		}
		return quadPatterns;
	}

	private QuadPatternTerm toTerm(Var var, ValueStoreFacade valueStore)
			throws QueryEvaluationException {
		if (var == null) {
			return QuadPatternTerm.unbound();
		}
		if (var.hasValue()) {
			return QuadPatternTerm.constant(valueStore.getId(var.getValue()));
		}
		if (var.getName() != null) {
			return QuadPatternTerm.variable(var.getName());
		}
		return QuadPatternTerm.unbound();
	}

	private final class BindingIterator implements Iterator<BindingSet>, AutoCloseable {

		private final AtomicBoolean cancelled;
		private int snapshotIndex;
		private Iterator<BindingSet> current = Collections.emptyIterator();
		private FrameCursor currentCursor;
		private RuntimeException error;
		private BindingSet next;

		BindingIterator(AtomicBoolean cancelled) {
			this.cancelled = cancelled;
		}

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			if (cancelled.get()) {
				return false;
			}
			if (error != null) {
				throw error;
			}

			try {
				while (true) {
					if (current != null) {
						if (current.hasNext()) {
							next = current.next();
							return true;
						} else {
							if (currentCursor != null) {
								currentCursor.close();
								currentCursor = null;
							}
							current = null;
						}
					}
					if (snapshotIndex >= snapshots.size()) {
						return false;
					}

					LmdbDatasetSnapshot snapshot = snapshots.get(snapshotIndex++);
					Map<QuadKeyOrder, Integer> indexHandles = snapshot.indexHandles();
					if (indexHandles == null || indexHandles.isEmpty()) {
						continue;
					}

					ValueStoreFacade valueStore = new ValueStoreFacade(snapshot);
					List<QuadPattern> quadPatterns = toQuadPatterns(wcoj.getPatterns(), valueStore);
					currentCursor = new FrameCursor(snapshot, valueStore, quadPatterns, indexHandles);
					current = currentCursor;
				}
			} catch (QueryEvaluationException e) {
				error = new RuntimeException(e);
				throw error;
			} catch (RuntimeException e) {
				error = e;
				throw e;
			}
		}

		@Override
		public BindingSet next() {
			if (!hasNext()) {
				throw new NoSuchElementException("No more results");
			}
			BindingSet result = next;
			next = null;
			return result;
		}

		@Override
		public void close() {
			if (currentCursor != null) {
				currentCursor.close();
			}
		}

		private Map<QuadPattern, QuadKeyOrder> chooseOrders(List<QuadPattern> patterns, List<String> order,
				Map<QuadKeyOrder, Integer> indexHandles) {
			List<QuadKeyOrder> candidates = new ArrayList<>(indexHandles.keySet());
			Map<QuadPattern, QuadKeyOrder> chosen = new HashMap<>();
			for (QuadPattern pattern : patterns) {
				QuadKeyOrder selected = IndexSelector.chooseBestOrder(pattern, order, candidates);
				chosen.put(pattern, selected);
			}
			return chosen;
		}

		private final class FrameCursor implements Iterator<BindingSet>, AutoCloseable {
			private final LmdbDatasetSnapshot snapshot;
			private final ValueStoreFacade valueStore;
			private final List<QuadPattern> patterns;
			private final Map<QuadKeyOrder, Integer> indexHandles;
			private final List<String> variableOrder;
			private final Map<QuadPattern, QuadKeyOrder> orders;
			private final PatternMetadata[] patternMetadata;
			private final List<VarParticipation>[] participatingByDepth;
			private final long[] bindingValues;
			private final boolean[] bindingPresent;
			private final ArrayDeque<Frame> stack = new ArrayDeque<>();
			private BindingSet pending;

			FrameCursor(LmdbDatasetSnapshot snapshot, ValueStoreFacade valueStore, List<QuadPattern> patterns,
					Map<QuadKeyOrder, Integer> indexHandles) throws QueryEvaluationException {
				this.snapshot = snapshot;
				this.valueStore = valueStore;
				this.patterns = patterns;
				this.indexHandles = indexHandles;
				this.variableOrder = LFTJExecutor.chooseVariableOrder(patterns, indexHandles.keySet());
				this.orders = chooseOrders(patterns, variableOrder, indexHandles);
				Precompiled precompiled = precompile(patterns, orders, variableOrder);
				this.patternMetadata = precompiled.patternMetadata();
				this.participatingByDepth = precompiled.participatingByVariable();
				this.bindingValues = new long[variableOrder.size()];
				this.bindingPresent = new boolean[variableOrder.size()];
				pushFrame(0);
			}

			@Override
			public boolean hasNext() {
				if (pending != null) {
					return true;
				}
				if (cancelled.get()) {
					return false;
				}
				try {
					while (!stack.isEmpty()) {
						Frame frame = stack.peek();
						if (!frame.initialized) {
							frame.initialize();
							if (frame.exhausted) {
								popFrame();
								continue;
							}
						}

						if (frame.nextValue()) {
							bindingValues[frame.variableIndex] = frame.currentValue;
							bindingPresent[frame.variableIndex] = true;
							if (stack.size() == variableOrder.size()) {
								pending = toBindingSet(variableOrder, bindingValues, bindingPresent, valueStore);
								return true;
							}
							pushFrame(stack.size());
							continue;
						}

						popFrame();
					}
				} catch (IOException | QueryEvaluationException e) {
					throw new RuntimeException(e);
				}
				return false;
			}

			@Override
			public BindingSet next() {
				if (!hasNext()) {
					throw new NoSuchElementException("No more results");
				}
				BindingSet bs = pending;
				pending = null;
				return bs;
			}

			private void pushFrame(int depth) {
				if (depth >= variableOrder.size()) {
					return;
				}
				stack.push(new Frame(depth));
			}

			private void popFrame() {
				Frame frame = stack.pop();
				bindingPresent[frame.variableIndex] = false;
				frame.close();
			}

			@Override
			public void close() {
				while (!stack.isEmpty()) {
					popFrame();
				}
			}

			private final class Frame {
				private final String variable;
				private final int variableIndex;
				private final List<VarParticipation> participating;
				private List<LMDBTrieIterator> iterators = List.of();
				private LeapfrogCursor cursor;
				private boolean initialized;
				private boolean exhausted;
				private long currentValue;

				Frame(int depth) {
					this.variable = variableOrder.get(depth);
					this.variableIndex = depth;
					this.participating = participatingByDepth[depth];
				}

				void initialize() throws IOException, QueryEvaluationException {
					if (initialized) {
						return;
					}
					List<LMDBTrieIterator> iters = new ArrayList<>(participating.size());
					for (VarParticipation participation : participating) {
						QuadPattern pattern = patterns.get(participation.patternIndex());
						QuadKeyOrder order = orders.get(pattern);
						Integer dbi = FrameCursor.this.indexHandles.get(order);
						if (dbi == null) {
							throw new IllegalStateException("No DBI registered for order " + order);
						}
						PatternMetadata metadata = patternMetadata[participation.patternIndex()];
						Prefix prefix = buildPrefix(metadata, variableIndex, bindingValues, bindingPresent);
						Slot slot = participation.slot();
						LMDBTrieIterator iterator = new LMDBTrieIterator(valueStore.txnId(), dbi.intValue(), order,
								slot);
						iterator.open(prefix);
						if (iterator.atEnd()) {
							closeIterators(iters);
							exhausted = true;
							initialized = true;
							return;
						}
						iters.add(iterator);
					}
					iterators = iters;
					cursor = new LeapfrogCursor(variable, iterators);
					if (!cursor.hasValue()) {
						exhausted = true;
					}
					initialized = true;
				}

				boolean nextValue() {
					if (exhausted || cursor == null) {
						return false;
					}
					if (!cursor.hasValue()) {
						exhausted = true;
						return false;
					}
					currentValue = cursor.current();
					cursor.advance();
					return true;
				}

				void close() {
					closeIterators(iterators);
				}
			}

			private final class LeapfrogCursor {
				private final LeapfrogIteratorCursor cursor;
				private final List<Slot> slots;
				private final String frameVariable;

				LeapfrogCursor(String frameVariable, List<LMDBTrieIterator> iterators) {
					this.cursor = new LeapfrogIteratorCursor(iterators);
					this.slots = new ArrayList<>(iterators.size());
					for (LMDBTrieIterator iterator : iterators) {
						this.slots.add(iterator.slot());
					}
					this.frameVariable = frameVariable;
					if (cursor.sawStalledSeek() && LOGGER.isDebugEnabled()) {
						LOGGER.debug("LFTJ leapfrog stalled on variable {} with slots {}", frameVariable,
								slots);
					}
				}

				boolean hasValue() {
					return cursor.hasValue();
				}

				long current() {
					return cursor.current();
				}

				void advance() {
					cursor.advance();
				}
			}

			private Prefix buildPrefix(PatternMetadata metadata, int currentIndex, long[] values, boolean[] present) {
				Prefix.Builder prefix = Prefix.builder();
				for (SlotDescriptor descriptor : metadata.slots()) {
					if (descriptor.isConstant()) {
						write(prefix, descriptor.slot(), descriptor.constant());
						continue;
					}
					if (descriptor.isVariable() && descriptor.variableId() < currentIndex
							&& present[descriptor.variableId()]) {
						write(prefix, descriptor.slot(), values[descriptor.variableId()]);
					}
				}
				return prefix.build();
			}

			private void write(Prefix.Builder prefix, Slot slot, long value) {
				switch (slot) {
				case S:
					prefix.subject(value);
					break;
				case P:
					prefix.predicate(value);
					break;
				case O:
					prefix.object(value);
					break;
				case C:
					prefix.context(value);
					break;
				default:
					throw new IllegalArgumentException("Unknown slot: " + slot);
				}
			}

			private Map<String, Integer> indexByName(List<String> order) {
				Map<String, Integer> index = new HashMap<>(order.size());
				for (int i = 0; i < order.size(); i++) {
					index.put(order.get(i), i);
				}
				return index;
			}

			private Precompiled precompile(List<QuadPattern> patterns, Map<QuadPattern, QuadKeyOrder> chosenOrders,
					List<String> variableOrder) {
				Map<String, Integer> variableIndex = indexByName(variableOrder);
				PatternMetadata[] metadata = new PatternMetadata[patterns.size()];
				@SuppressWarnings("unchecked")
				List<VarParticipation>[] participating = new List[variableOrder.size()];

				for (int i = 0; i < patterns.size(); i++) {
					QuadPattern pattern = patterns.get(i);
					QuadKeyOrder order = chosenOrders.get(pattern);
					SlotDescriptor[] slots = new SlotDescriptor[Slot.values().length];
					for (Slot slot : Slot.values()) {
						QuadPatternTerm term = pattern.term(slot);
						if (term.isConstant()) {
							slots[slot.ordinal()] = new SlotDescriptor(slot, true, false, term.constant(), -1);
							continue;
						}
						if (term.isVariable()) {
							Integer varId = variableIndex.get(term.variable());
							if (varId == null) {
								throw new IllegalArgumentException("Variable not present in order: " + term.variable());
							}
							slots[slot.ordinal()] = new SlotDescriptor(slot, false, true, 0L, varId.intValue());
							participating[varId.intValue()] = addParticipation(participating[varId.intValue()], i,
									slot);
							continue;
						}
						slots[slot.ordinal()] = new SlotDescriptor(slot, false, false, 0L, -1);
					}
					metadata[i] = new PatternMetadata(order, slots);
				}

				for (int i = 0; i < participating.length; i++) {
					if (participating[i] == null) {
						participating[i] = List.of();
					}
				}

				return new Precompiled(metadata, participating);
			}

			private List<VarParticipation> addParticipation(List<VarParticipation> existing, int patternIndex,
					Slot slot) {
				List<VarParticipation> list = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
				list.add(new VarParticipation(patternIndex, slot));
				return list;
			}

			private final class Precompiled {
				private final PatternMetadata[] patternMetadata;
				private final List<VarParticipation>[] participatingByVariable;

				Precompiled(PatternMetadata[] patternMetadata, List<VarParticipation>[] participatingByVariable) {
					this.patternMetadata = patternMetadata;
					this.participatingByVariable = participatingByVariable;
				}

				PatternMetadata[] patternMetadata() {
					return patternMetadata;
				}

				List<VarParticipation>[] participatingByVariable() {
					return participatingByVariable;
				}
			}

			private final class PatternMetadata {
				private final QuadKeyOrder order;
				private final SlotDescriptor[] slots;

				PatternMetadata(QuadKeyOrder order, SlotDescriptor[] slots) {
					this.order = order;
					this.slots = slots;
				}

				QuadKeyOrder order() {
					return order;
				}

				SlotDescriptor[] slots() {
					return slots;
				}
			}

			private final class VarParticipation {
				private final int patternIndex;
				private final Slot slot;

				VarParticipation(int patternIndex, Slot slot) {
					this.patternIndex = patternIndex;
					this.slot = slot;
				}

				int patternIndex() {
					return patternIndex;
				}

				Slot slot() {
					return slot;
				}
			}

			private final class SlotDescriptor {
				private final Slot slot;
				private final boolean constant;
				private final boolean variable;
				private final long constantValue;
				private final int variableId;

				private SlotDescriptor(Slot slot, boolean constant, boolean variable, long constantValue,
						int variableId) {
					this.slot = slot;
					this.constant = constant;
					this.variable = variable;
					this.constantValue = constantValue;
					this.variableId = variableId;
				}

				Slot slot() {
					return slot;
				}

				boolean isConstant() {
					return constant;
				}

				boolean isVariable() {
					return variable;
				}

				long constant() {
					return constantValue;
				}

				int variableId() {
					return variableId;
				}
			}

			private void closeIterators(List<LMDBTrieIterator> iterators) {
				for (LMDBTrieIterator iterator : iterators) {
					try {
						iterator.close();
					} catch (Exception e) {
						// ignore close failures
					}
				}
			}
		}
	}

	private final class SingleThreadWCOJIteration extends CloseableIteratorIteration<BindingSet> {

		private final AtomicBoolean cancelled;
		private final BindingIterator iterator;

		SingleThreadWCOJIteration(BindingIterator iterator, AtomicBoolean cancelled) {
			super(iterator);
			this.iterator = iterator;
			this.cancelled = cancelled;
		}

		@Override
		protected void handleClose() {
			cancelled.set(true);
			iterator.close();
		}
	}

	private static final class ValueStoreFacade {
		private final LmdbDatasetSnapshot snapshot;

		ValueStoreFacade(LmdbDatasetSnapshot snapshot) {
			this.snapshot = snapshot;
		}

		long txnId() throws QueryEvaluationException {
			try {
				return snapshot.getTxn().get();
			} catch (Exception e) {
				throw new QueryEvaluationException(e);
			}
		}

		long getId(Value value) throws QueryEvaluationException {
			try {
				return snapshot.valueStore().getId(value);
			} catch (IOException e) {
				throw new QueryEvaluationException(e);
			}
		}

		Value getValue(long id) throws IOException {
			return snapshot.valueStore().getValue(id);
		}

	}
}
