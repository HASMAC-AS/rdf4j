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
package org.eclipse.rdf4j.sail.lmdb.lftj;

import java.util.Objects;

/**
 * Represents a single quad component inside a {@link QuadPattern}. The component is either a constant term id, a
 * named query variable, or an unbound wildcard slot.
 */
public final class QuadPatternTerm {
        private final Long constant;
        private final String variable;

        private QuadPatternTerm(Long constant, String variable) {
                this.constant = constant;
                this.variable = variable;
        }

        public static QuadPatternTerm constant(long value) {
                return new QuadPatternTerm(value, null);
        }

        public static QuadPatternTerm variable(String name) {
                Objects.requireNonNull(name, "name");
                return new QuadPatternTerm(null, name);
        }

        public static QuadPatternTerm unbound() {
                return new QuadPatternTerm(null, null);
        }

        public boolean isConstant() {
                return constant != null;
        }

        public boolean isVariable() {
                return variable != null;
        }

        public boolean isUnbound() {
                return constant == null && variable == null;
        }

        public long constant() {
                if (constant == null) {
                        throw new IllegalStateException("Term is not a constant");
                }
                return constant;
        }

        public String variable() {
                if (variable == null) {
                        throw new IllegalStateException("Term is not a variable");
                }
                return variable;
        }
}
