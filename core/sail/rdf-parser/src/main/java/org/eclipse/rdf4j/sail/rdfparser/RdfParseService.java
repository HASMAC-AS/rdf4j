/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.rdfparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.impl.MapBindingSet;

/**
 * A local SERVICE implementation. Each correlated invocation owns one immutable parsed dataset, kept only by its
 * result iterator. It neither writes to the parent Sail nor shares graphs between invocations, queries or users.
 */
public final class RdfParseService implements FederatedService {
	private final RdfDocumentParser parser;
	private final Map<IRI, RdfParseTupleFunction> functions;

	RdfParseService(RdfDocumentParser parser, Map<IRI, RdfParseTupleFunction> functions) {
		this.parser = parser;
		this.functions = functions;
	}

	@Override
	public boolean ask(Service service, BindingSet bindings, String baseUri) {
		try (CloseableIteration<BindingSet> result = query(ServiceRequest.from(service), bindings)) {
			return result.hasNext();
		}
	}

	@Override
	public CloseableIteration<BindingSet> select(Service service, Set<String> projectionVars, BindingSet bindings,
			String baseUri) {
		// Parse failures occur here, before even the first row is returned to the outer SERVICE operator.
		CloseableIteration<BindingSet> result = query(ServiceRequest.from(service), bindings);
		return projected(result, projectionVars, bindings);
	}

	private CloseableIteration<BindingSet> query(ServiceRequest request, BindingSet bindings) {
		Model model = parser.parse(request.value(request.content(), bindings),
				request.value(request.format(), bindings), request.value(request.base(), bindings));
		ParsedDatasetSource source = new ParsedDatasetSource(model);
		var dataset = source.dataset();
		DefaultEvaluationStrategy strategy = new DefaultEvaluationStrategy(source, dataset, endpoint -> {
			throw new QueryEvaluationException("Nested federation is disabled for parsed RDF");
		});
		TupleExpr body = PropertyFunctionRewriter.rewrite(request.body(), functions);
		body = strategy.optimize(body, new EvaluationStatistics(), bindings);
		return strategy.precompile(body).evaluate(bindings);
	}

	private static CloseableIteration<BindingSet> projected(CloseableIteration<BindingSet> result,
			Set<String> variables, BindingSet bindings) {
		return new LookAheadIteration<>() {
			private CloseableIteration<BindingSet> delegate = result;

			@Override
			protected BindingSet getNextElement() {
				try {
					while (delegate.hasNext()) {
						BindingSet row = delegate.next();
						MapBindingSet merged = new MapBindingSet(bindings.size() + variables.size());
						for (Binding binding : bindings) {
							merged.addBinding(binding);
						}
						boolean compatible = true;
						for (String variable : variables) {
							var value = row.getValue(variable);
							if (value != null) {
								var existing = merged.getValue(variable);
								if (existing != null && !existing.equals(value)) {
									compatible = false;
									break;
								}
								if (existing == null) {
									merged.addBinding(variable, value);
								}
							}
						}
						if (compatible) {
							return merged;
						}
					}
					return null;
				} catch (RuntimeException | Error failure) {
					closeWithSuppressed(this, failure);
					throw failure;
				}
			}

			@Override
			protected void handleClose() {
				var previous = delegate;
				delegate = null;
				previous.close();
			}
		};
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(Service service, CloseableIteration<BindingSet> inputs,
			String baseUri) {
		// Do not collapse duplicate inputs into a set. Each row is a distinct correlated invocation.
		return new LookAheadIteration<>() {
			private CloseableIteration<BindingSet> current;

			@Override
			protected BindingSet getNextElement() {
				try {
					while (true) {
						if (current != null && current.hasNext()) {
							return current.next();
						}
						if (current != null) {
							current.close();
							current = null;
						}
						if (!inputs.hasNext()) {
							return null;
						}
						BindingSet input = inputs.next();
						if (!service.isSilent()) {
							current = select(service, service.getServiceVars(), input, baseUri);
						} else {
							// SILENT is all-or-nothing for this invocation, including lazy evaluation failures.
							try (var rows = select(service, service.getServiceVars(), input, baseUri)) {
								List<BindingSet> completed = new ArrayList<>();
								while (rows.hasNext()) {
									completed.add(rows.next());
								}
								current = new CloseableIteratorIteration<>(completed.iterator());
							} catch (QueryEvaluationException failure) {
								current = new SingletonIteration<>(input);
							}
						}
					}
				} catch (RuntimeException | Error failure) {
					closeWithSuppressed(this, failure);
					throw failure;
				}
			}

			@Override
			protected void handleClose() {
				try {
					if (current != null) {
						current.close();
						current = null;
					}
				} finally {
					inputs.close();
				}
			}
		};
	}

	static void closeWithSuppressed(CloseableIteration<?> iteration, Throwable failure) {
		try {
			iteration.close();
		} catch (RuntimeException | Error closeFailure) {
			if (failure != closeFailure) {
				failure.addSuppressed(closeFailure);
			}
		}
	}

	@Override
	public boolean isInitialized() {
		return true; // This service owns no shared mutable resources.
	}

	@Override
	public void initialize() {
	}

	@Override
	public void shutdown() {
	}
}
