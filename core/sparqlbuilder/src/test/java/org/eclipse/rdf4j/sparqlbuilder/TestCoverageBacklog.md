<!--
Copyright (c) 2025 Eclipse RDF4J contributors.

All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.

SPDX-License-Identifier: BSD-3-Clause
-->
# SparqlBuilder Test Coverage Backlog

The following test cases from the agreed plan are **not yet implemented**.

## Query skeleton & clauses
- (complete) Q-WH series and Q-CON series now covered.

## VALUES
- VAL-05: Empty row set handling (allowed/disallowed behaviour).

## Generator helpers
- (complete) GEN-01 and GEN-02 now covered by `GeneratorHelpersTest`.

## Graph patterns
- GP-SS-01: Subselect via `GraphPatterns.select(...).where(...).groupBy(...).having(...)`.

## RDF terms & predicate-object lists
- (complete) RDF-TP-01 through RDF-TP-04 via `TriplePatternBuilderTest`.
- (complete) RDF-VAL-01 through RDF-VAL-09 via `RdfTermConstructionTest`.

## Property paths
- PATH-01 through PATH-09.

## Expressions, functions, aggregates, BIND
- EXPR-AR-01, EXPR-BO-01, EXPR-CMP-01, EXPR-IN-01, EXPR-NIN-01.
- EXPR-FN-STR-01 through EXPR-FN-STR-04.
- EXPR-FN-DT-01, EXPR-FN-BNODE-01, EXPR-FN-BOUND-01, EXPR-FN-COALESCE-01, EXPR-FN-NUM-01.
- EXPR-AGG-01 through EXPR-AGG-04.
- EXPR-BIND-01, EXPR-BIND-02.
- EXPR-CUST-01.

## SPARQL Update
- UPD-DATA-01 through UPD-DATA-03.
- UPD-MOD-01 through UPD-MOD-05.
- UPD-LOAD-01, UPD-LOAD-02.
- UPD-CLR-01 through UPD-CLR-05.
- UPD-CRD-01, UPD-DRP-01.
- UPD-COPY-01, UPD-MOVE-01, UPD-ADD-01.

## Availability & negatives
- NEG-ASK-01, NEG-DESC-01.
- NEG-VAL-01 (arity mismatch already covered) – confirm semantics once broader suite stabilises.
- NEG-PATH-01.
- NEG-EXP-01.

## Combinatorial suite
- CT-01 through CT-10.

## Subselects
- SS-01, SS-02.

## End-to-end interaction
- IT-01 through IT-04.

## Stability & regression sentinels
- STA-01 through STA-05.

## Known limitations & placeholders
- LIM-ASK, LIM-SERVICE.
