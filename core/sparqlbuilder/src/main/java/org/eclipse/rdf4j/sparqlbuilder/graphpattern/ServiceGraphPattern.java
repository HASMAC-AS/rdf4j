/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

class ServiceGraphPattern extends GroupGraphPattern {

	private static final String SERVICE = "SERVICE ";
	private static final String SILENT = "SILENT ";

	private final GraphName serviceName;
	private final boolean silent;

	ServiceGraphPattern(GraphName serviceName, boolean silent) {
		this.serviceName = serviceName;
		this.silent = silent;
	}

	@Override
	public String getQueryString() {
		StringBuilder builder = new StringBuilder(SERVICE);
		if (silent) {
			builder.append(SILENT);
		}
		builder.append(serviceName.getQueryString()).append(" ");
		builder.append(super.getQueryString());
		return builder.toString();
	}
}
