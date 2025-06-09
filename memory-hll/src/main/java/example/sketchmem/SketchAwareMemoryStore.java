/*****************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *****************************************************************************/
package example.sketchmem;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;

/**
 * MemoryStore that records HyperLogLog sketches for inserted statements.
 */
public class SketchAwareMemoryStore extends MemoryStore {
	private final SketchRegistry reg;

	/**
	 * Create a new store using the provided registry.
	 *
	 * @param r registry to store sketches
	 */
	public SketchAwareMemoryStore(SketchRegistry r) {
		this.reg = r;
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new SketchAwareConnection(this);
	}

	private class SketchAwareConnection extends MemoryStoreConnection {
		SketchAwareConnection(SketchAwareMemoryStore sail) {
			super(sail);
		}

		@Override
		protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
				throws SailException {
			super.addStatementInternal(subj, pred, obj, contexts);
			reg.forPred(pred.stringValue()).update(subj, obj);
		}
	}
}
