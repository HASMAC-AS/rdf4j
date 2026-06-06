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
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.Objects;

import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * Collects non-constant variable names into a query-local integer mask.
 */
final class VarNameMaskCollector extends AbstractSimpleQueryModelVisitor<RuntimeException> {

	private final VarNameMap varNames;
	private final VarNameMask.Builder builder = VarNameMask.builder();

	private VarNameMaskCollector(VarNameMap varNames) {
		super(true);
		this.varNames = Objects.requireNonNull(varNames, "varNames must not be null");
	}

	static VarNameMask process(QueryModelNode node, VarNameMap varNames) {
		Objects.requireNonNull(node, "node must not be null");

		VarNameMaskCollector collector = new VarNameMaskCollector(varNames);
		node.visit(collector);
		return collector.getVarNames();
	}

	VarNameMask getVarNames() {
		return builder.build();
	}

	@Override
	public void meet(Var var) {
		if (!var.hasValue()) {
			builder.add(varNames.idOf(var.getName()));
		}
	}
}
