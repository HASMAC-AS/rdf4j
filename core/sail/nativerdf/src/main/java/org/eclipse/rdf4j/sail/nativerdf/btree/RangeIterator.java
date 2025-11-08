/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;

class RangeIterator implements RecordIterator, NodeListener {

	private final BTree tree;

	private final byte[] minValue;

	private final byte[] maxValue;

	private final ValueMatcher valueMatcher;

	private volatile boolean started;

	private volatile Node currentNode;

	private final AtomicBoolean revisitValue = new AtomicBoolean();

	/**
	 * Tracks the parent nodes of {@link #currentNode}.
	 */
	private final LinkedList<Node> parentNodeStack = new LinkedList<>();

	/**
	 * Tracks the index of child nodes in parent nodes.
	 */
	private final LinkedList<Integer> parentIndexStack = new LinkedList<>();

	private volatile int currentIdx;

	private volatile boolean closed = false;

	public RangeIterator(BTree tree, byte[] searchKey, byte[] searchMask, byte[] minValue, byte[] maxValue) {
		this.tree = tree;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.valueMatcher = ValueMatcher.create(searchKey, searchMask);
		this.started = false;
	}

	@Override
	public byte[] next() throws IOException {
		tree.btreeLock.readLock().lock();
		try {
			if (!started) {
				started = true;
				findMinimum();
			}

			byte[] value = findNext(revisitValue.getAndSet(false));
			while (value != null) {
				if (maxValue != null && tree.comparator.compareBTreeValues(maxValue, value, 0, value.length) < 0) {
					// Reached maximum value, stop iterating
					close();
					value = null;
					break;
				} else if (!valueMatcher.matches(value)) {
					// Value doesn't match search key/mask
					value = findNext(false);
					continue;
				} else {
					// Matching value found
					break;
				}
			}

			return value;
		} finally {
			tree.btreeLock.readLock().unlock();
		}
	}

	private void findMinimum() {
		Node nextCurrentNode = currentNode = tree.readRootNode();

		if (nextCurrentNode == null) {
			// Empty BTree
			return;
		}

		nextCurrentNode.register(this);
		currentIdx = 0;

		// Search first value >= minValue, or the left-most value in case
		// minValue is null
		while (true) {
			if (minValue != null) {
				currentIdx = nextCurrentNode.search(minValue);

				if (currentIdx >= 0) {
					// Found exact match with minimum value
					break;
				} else {
					// currentIdx indicates the first value larger than the
					// minimum value
					currentIdx = -currentIdx - 1;
				}
			}

			if (nextCurrentNode.isLeaf()) {
				break;
			} else {
				// [SES-725] must change stacks after node loading has succeeded
				Node childNode = nextCurrentNode.getChildNode(currentIdx);
				pushStacks(childNode);
				// pushStacks updates the current node
				nextCurrentNode = currentNode;
			}
		}
	}

	private byte[] findNext(boolean returnedFromRecursion) throws IOException {
		Node nextCurrentNode = currentNode;
		if (nextCurrentNode == null) {
			return null;
		}

		if (returnedFromRecursion || nextCurrentNode.isLeaf()) {
			if (currentIdx >= nextCurrentNode.getValueCount()) {
				// No more values in this node, continue with parent node
				popStacks();
				return findNext(true);
			} else {
				return nextCurrentNode.getValue(currentIdx++);
			}
		} else {
			// [SES-725] must change stacks after node loading has succeeded
			Node childNode = nextCurrentNode.getChildNode(currentIdx);
			pushStacks(childNode);
			return findNext(false);
		}
	}

	@Override
	public void set(byte[] value) {
		tree.btreeLock.readLock().lock();
		try {
			Node nextCurrentNode = currentNode;
			if (nextCurrentNode == null || currentIdx > nextCurrentNode.getValueCount()) {
				throw new IllegalStateException();
			}

			nextCurrentNode.setValue(currentIdx - 1, value);
		} finally {
			tree.btreeLock.readLock().unlock();
		}
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			synchronized (this) {
				if (!closed) {
					closed = true;
					tree.btreeLock.readLock().lock();
					try {
						while (popStacks()) {
						}

						assert parentNodeStack.isEmpty();
						assert parentIndexStack.isEmpty();
					} finally {
						tree.btreeLock.readLock().unlock();
					}
				}
			}
		}
	}

	private void pushStacks(Node newChildNode) {
		newChildNode.register(this);
		parentNodeStack.add(currentNode);
		parentIndexStack.add(currentIdx);
		currentNode = newChildNode;
		currentIdx = 0;
	}

	private synchronized boolean popStacks() throws IOException {
		Node nextCurrentNode = currentNode;
		if (nextCurrentNode == null) {
			// There's nothing to pop
			return false;
		}

		nextCurrentNode.deregister(this);
		nextCurrentNode.release();

		if (!parentNodeStack.isEmpty()) {
			currentNode = parentNodeStack.removeLast();
			currentIdx = parentIndexStack.removeLast();
			return true;
		} else {
			currentNode = null;
			currentIdx = 0;
			return false;
		}
	}

	@Override
	public boolean valueAdded(Node node, int addedIndex) {
		assert tree.btreeLock.isWriteLockedByCurrentThread();

		if (node == currentNode) {
			if (addedIndex < currentIdx) {
				currentIdx++;
			}
		} else {
			for (int i = 0; i < parentNodeStack.size(); i++) {
				if (node == parentNodeStack.get(i)) {
					int parentIdx = parentIndexStack.get(i);
					if (addedIndex < parentIdx) {
						parentIndexStack.set(i, parentIdx + 1);
					}

					break;
				}
			}
		}

		return false;
	}

	@Override
	public boolean valueRemoved(Node node, int removedIndex) {
		assert tree.btreeLock.isWriteLockedByCurrentThread();

		if (node == currentNode) {
			if (removedIndex < currentIdx) {
				currentIdx--;
			}
		} else {
			for (int i = 0; i < parentNodeStack.size(); i++) {
				if (node == parentNodeStack.get(i)) {
					int parentIdx = parentIndexStack.get(i);
					if (removedIndex < parentIdx) {
						parentIndexStack.set(i, parentIdx - 1);
					}

					break;
				}
			}
		}

		return false;
	}

	@Override
	public boolean rotatedLeft(Node node, int valueIndex, Node leftChildNode, Node rightChildNode) throws IOException {
		Node nextCurrentNode = currentNode;
		if (nextCurrentNode == node) {
			if (valueIndex == currentIdx - 1) {
				// the value that was removed had just been visited
				currentIdx = valueIndex;
				revisitValue.set(true);

				if (!node.isLeaf()) {
					pushStacks(leftChildNode);
					leftChildNode.use();
				}
			}
		} else if (nextCurrentNode == rightChildNode) {
			if (currentIdx == 0) {
				// the value that would be visited next has been moved to the
				// parent node
				popStacks();
				currentIdx = valueIndex;
				revisitValue.set(true);
			}
		} else {
			for (int i = 0; i < parentNodeStack.size(); i++) {
				Node stackNode = parentNodeStack.get(i);

				if (stackNode == rightChildNode) {
					int stackIdx = parentIndexStack.get(i);

					if (stackIdx == 0) {
						// this node is no longer the parent, replace with left
						// sibling
						rightChildNode.deregister(this);
						rightChildNode.release();

						leftChildNode.use();
						leftChildNode.register(this);

						parentNodeStack.set(i, leftChildNode);
						parentIndexStack.set(i, leftChildNode.getValueCount());
					}

					break;
				}
			}
		}

		return false;
	}

	@Override
	public boolean rotatedRight(Node node, int valueIndex, Node leftChildNode, Node rightChildNode) throws IOException {
		for (int i = 0; i < parentNodeStack.size(); i++) {
			Node stackNode = parentNodeStack.get(i);

			if (stackNode == leftChildNode) {
				int stackIdx = parentIndexStack.get(i);

				if (stackIdx == leftChildNode.getValueCount()) {
					// this node is no longer the parent, replace with right
					// sibling
					leftChildNode.deregister(this);
					leftChildNode.release();

					rightChildNode.use();
					rightChildNode.register(this);

					parentNodeStack.set(i, rightChildNode);
					parentIndexStack.set(i, 0);
				}

				break;
			}
		}

		return false;
	}

	@Override
	public boolean nodeSplit(Node node, Node newNode, int medianIdx) throws IOException {
		assert tree.btreeLock.isWriteLockedByCurrentThread();

		boolean deregister = false;

		Node nextCurrentNode = currentNode;
		if (node == nextCurrentNode) {
			if (currentIdx > medianIdx) {
				nextCurrentNode.release();
				deregister = true;

				newNode.use();
				newNode.register(this);

				currentNode = newNode;
				currentIdx -= medianIdx + 1;
			}
		} else {
			for (int i = 0; i < parentNodeStack.size(); i++) {
				Node parentNode = parentNodeStack.get(i);

				if (node == parentNode) {
					int parentIdx = parentIndexStack.get(i);

					if (parentIdx > medianIdx) {
						parentNode.release();
						deregister = true;

						newNode.use();
						newNode.register(this);

						parentNodeStack.set(i, newNode);
						parentIndexStack.set(i, parentIdx - medianIdx - 1);
					}

					break;
				}
			}
		}

		return deregister;
	}

	@Override
	public boolean nodeMergedWith(Node sourceNode, Node targetNode, int mergeIdx) throws IOException {
		assert tree.btreeLock.isWriteLockedByCurrentThread();

		boolean deregister = false;

		Node nextCurrentNode = currentNode;
		if (sourceNode == nextCurrentNode) {
			nextCurrentNode.release();
			deregister = true;

			targetNode.use();
			targetNode.register(this);

			currentNode = targetNode;
			currentIdx += mergeIdx;
		} else {
			for (int i = 0; i < parentNodeStack.size(); i++) {
				Node parentNode = parentNodeStack.get(i);

				if (sourceNode == parentNode) {
					parentNode.release();
					deregister = true;

					targetNode.use();
					targetNode.register(this);

					parentNodeStack.set(i, targetNode);
					parentIndexStack.set(i, mergeIdx + parentIndexStack.get(i));

					break;
				}
			}
		}

		return deregister;
	}

	@Override
	public String toString() {
		return "RangeIterator{" +
				"tree=" + tree +
				'}';
	}

	static final class ValueMatcher {

		private static final int RECORD_LENGTH = 17;

		private static final int SUBJECT_OFFSET = 0;

		private static final int PREDICATE_OFFSET = 4;

		private static final int OBJECT_OFFSET = 8;

		private static final int CONTEXT_OFFSET = 12;

		private static final int FLAG_OFFSET = 16;

		private static final int SUBJECT_BIT = 0b0001;

		private static final int PREDICATE_BIT = 0b0010;

		private static final int OBJECT_BIT = 0b0100;

		private static final int CONTEXT_BIT = 0b1000;

		private static final ValueMatcher MATCH_ALL = new ValueMatcher(value -> true);

		private final MatchFn matcher;

		private final int subject;

		private final int predicate;

		private final int object;

		private final int context;

		private final int flagMask;

		private final int flagValue;

		private ValueMatcher(MatchFn matcher) {
			this.matcher = matcher;
			this.subject = 0;
			this.predicate = 0;
			this.object = 0;
			this.context = 0;
			this.flagMask = 0;
			this.flagValue = 0;
		}

		private ValueMatcher(boolean matchSubject, boolean matchPredicate, boolean matchObject, boolean matchContext,
				int subject, int predicate, int object, int context, int flagMask, int flagValue) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
			this.context = context;
			this.flagMask = flagMask;
			this.flagValue = flagValue;
			this.matcher = selectMatchFn(matchSubject, matchPredicate, matchObject, matchContext);
		}

		static ValueMatcher create(byte[] searchKey, byte[] searchMask) {
			if (searchKey == null || searchMask == null) {
				return MATCH_ALL;
			}
			if (searchKey.length != RECORD_LENGTH || searchMask.length != RECORD_LENGTH) {
				return new ValueMatcher(value -> ByteArrayUtil.matchesPattern(value, searchMask, searchKey));
			}

			boolean matchSubject = hasMask(searchMask, SUBJECT_OFFSET);
			boolean matchPredicate = hasMask(searchMask, PREDICATE_OFFSET);
			boolean matchObject = hasMask(searchMask, OBJECT_OFFSET);
			boolean matchContext = hasMask(searchMask, CONTEXT_OFFSET);

			if ((matchSubject && !isFullMask(searchMask, SUBJECT_OFFSET))
					|| (matchPredicate && !isFullMask(searchMask, PREDICATE_OFFSET))
					|| (matchObject && !isFullMask(searchMask, OBJECT_OFFSET))
					|| (matchContext && !isFullMask(searchMask, CONTEXT_OFFSET))) {
				return new ValueMatcher(value -> ByteArrayUtil.matchesPattern(value, searchMask, searchKey));
			}

			int flagMask = Byte.toUnsignedInt(searchMask[FLAG_OFFSET]);
			int flagValue = Byte.toUnsignedInt(searchKey[FLAG_OFFSET]);

			if (!matchSubject && !matchPredicate && !matchObject && !matchContext && flagMask == 0) {
				return MATCH_ALL;
			}

			return new ValueMatcher(matchSubject, matchPredicate, matchObject, matchContext,
					ByteArrayUtil.getInt(searchKey, SUBJECT_OFFSET),
					ByteArrayUtil.getInt(searchKey, PREDICATE_OFFSET),
					ByteArrayUtil.getInt(searchKey, OBJECT_OFFSET),
					ByteArrayUtil.getInt(searchKey, CONTEXT_OFFSET),
					flagMask, flagValue);
		}

		boolean matches(byte[] value) {
			return matcher.matches(value);
		}

		private MatchFn selectMatchFn(boolean matchSubject, boolean matchPredicate, boolean matchObject,
				boolean matchContext) {
			int mask = 0;
			if (matchSubject) {
				mask |= SUBJECT_BIT;
			}
			if (matchPredicate) {
				mask |= PREDICATE_BIT;
			}
			if (matchObject) {
				mask |= OBJECT_BIT;
			}
			if (matchContext) {
				mask |= CONTEXT_BIT;
			}

			switch (mask) {
			case 0:
				return this::matchNone;
			case SUBJECT_BIT:
				return this::matchS;
			case PREDICATE_BIT:
				return this::matchP;
			case SUBJECT_BIT | PREDICATE_BIT:
				return this::matchSP;
			case OBJECT_BIT:
				return this::matchO;
			case SUBJECT_BIT | OBJECT_BIT:
				return this::matchSO;
			case PREDICATE_BIT | OBJECT_BIT:
				return this::matchPO;
			case SUBJECT_BIT | PREDICATE_BIT | OBJECT_BIT:
				return this::matchSPO;
			case CONTEXT_BIT:
				return this::matchC;
			case SUBJECT_BIT | CONTEXT_BIT:
				return this::matchSC;
			case PREDICATE_BIT | CONTEXT_BIT:
				return this::matchPC;
			case SUBJECT_BIT | PREDICATE_BIT | CONTEXT_BIT:
				return this::matchSPC;
			case OBJECT_BIT | CONTEXT_BIT:
				return this::matchOC;
			case SUBJECT_BIT | OBJECT_BIT | CONTEXT_BIT:
				return this::matchSOC;
			case PREDICATE_BIT | OBJECT_BIT | CONTEXT_BIT:
				return this::matchPOC;
			case SUBJECT_BIT | PREDICATE_BIT | OBJECT_BIT | CONTEXT_BIT:
				return this::matchSPOC;
			default:
				throw new IllegalStateException("Unsupported matcher mask: " + mask);
			}
		}

		private boolean matchNone(byte[] value) {
			return flagsMatch(value);
		}

		private boolean matchS(byte[] value) {
			return subjectMatches(value) && flagsMatch(value);
		}

		private boolean matchP(byte[] value) {
			return predicateMatches(value) && flagsMatch(value);
		}

		private boolean matchSP(byte[] value) {
			return subjectMatches(value) && predicateMatches(value) && flagsMatch(value);
		}

		private boolean matchO(byte[] value) {
			return objectMatches(value) && flagsMatch(value);
		}

		private boolean matchSO(byte[] value) {
			return subjectMatches(value) && objectMatches(value) && flagsMatch(value);
		}

		private boolean matchPO(byte[] value) {
			return predicateMatches(value) && objectMatches(value) && flagsMatch(value);
		}

		private boolean matchSPO(byte[] value) {
			return subjectMatches(value) && predicateMatches(value) && objectMatches(value) && flagsMatch(value);
		}

		private boolean matchC(byte[] value) {
			return contextMatches(value) && flagsMatch(value);
		}

		private boolean matchSC(byte[] value) {
			return subjectMatches(value) && contextMatches(value) && flagsMatch(value);
		}

		private boolean matchPC(byte[] value) {
			return predicateMatches(value) && contextMatches(value) && flagsMatch(value);
		}

		private boolean matchSPC(byte[] value) {
			return subjectMatches(value) && predicateMatches(value) && contextMatches(value) && flagsMatch(value);
		}

		private boolean matchOC(byte[] value) {
			return objectMatches(value) && contextMatches(value) && flagsMatch(value);
		}

		private boolean matchSOC(byte[] value) {
			return subjectMatches(value) && objectMatches(value) && contextMatches(value) && flagsMatch(value);
		}

		private boolean matchPOC(byte[] value) {
			return predicateMatches(value) && objectMatches(value) && contextMatches(value) && flagsMatch(value);
		}

		private boolean matchSPOC(byte[] value) {
			return subjectMatches(value) && predicateMatches(value) && objectMatches(value) && contextMatches(value)
					&& flagsMatch(value);
		}

		private boolean subjectMatches(byte[] value) {
			return ByteArrayUtil.getInt(value, SUBJECT_OFFSET) == subject;
		}

		private boolean predicateMatches(byte[] value) {
			return ByteArrayUtil.getInt(value, PREDICATE_OFFSET) == predicate;
		}

		private boolean objectMatches(byte[] value) {
			return ByteArrayUtil.getInt(value, OBJECT_OFFSET) == object;
		}

		private boolean contextMatches(byte[] value) {
			return ByteArrayUtil.getInt(value, CONTEXT_OFFSET) == context;
		}

		private boolean flagsMatch(byte[] value) {
			if (flagMask == 0) {
				return true;
			}
			int candidate = Byte.toUnsignedInt(value[FLAG_OFFSET]);
			return ((candidate ^ flagValue) & flagMask) == 0;
		}

		private static boolean hasMask(byte[] mask, int offset) {
			return ByteArrayUtil.getInt(mask, offset) != 0;
		}

		private static boolean isFullMask(byte[] mask, int offset) {
			return ByteArrayUtil.getInt(mask, offset) == -1;
		}

		@FunctionalInterface
		private interface MatchFn {
			boolean matches(byte[] value);
		}
	}
}
