/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;

/**
 * Precomputes reusable comparator and pattern score lambdas for {@link TripleStore} index orderings so that the
 * frequently executed code paths avoid repeatedly decoding the field sequence.
 */
final class TripleOrderFunctions {

	@FunctionalInterface
	interface CompareFn {
		int compare(byte[] key, byte[] data, int offset);
	}

	@FunctionalInterface
	interface PatternScoreFn {
		int score(int subj, int pred, int obj, int context);
	}

	private static final ConcurrentMap<String, CompareFn> COMPARATOR_CACHE = new ConcurrentHashMap<>();
	private static final ConcurrentMap<String, PatternScoreFn> PATTERN_SCORE_CACHE = new ConcurrentHashMap<>();

	private TripleOrderFunctions() {
		// utility
	}

	static CompareFn comparatorFor(char[] fieldSeq) {
		String key = new String(fieldSeq);
		return COMPARATOR_CACHE.computeIfAbsent(key, TripleOrderFunctions::buildComparator);
	}

	static PatternScoreFn patternScoreFor(char[] fieldSeq) {
		String key = new String(fieldSeq);
		return PATTERN_SCORE_CACHE.computeIfAbsent(key, TripleOrderFunctions::buildPatternScore);
	}

	private static CompareFn buildComparator(String fieldSeq) {
		int[] fieldOffsets = toOffsets(fieldSeq.toCharArray());
		if (fieldOffsets.length == 0) {
			return (key, data, offset) -> 0;
		}
		if (fieldOffsets.length == 1) {
			int first = fieldOffsets[0];
			return (key, data, offset) -> ByteArrayUtil.compareRegion(key, first, data, offset + first, 4);
		}
		if (fieldOffsets.length == 2) {
			int first = fieldOffsets[0];
			int second = fieldOffsets[1];
			return (key, data, offset) -> {
				int diff = ByteArrayUtil.compareRegion(key, first, data, offset + first, 4);
				if (diff != 0) {
					return diff;
				}
				return ByteArrayUtil.compareRegion(key, second, data, offset + second, 4);
			};
		}
		if (fieldOffsets.length == 3) {
			int first = fieldOffsets[0];
			int second = fieldOffsets[1];
			int third = fieldOffsets[2];
			return (key, data, offset) -> {
				int diff = ByteArrayUtil.compareRegion(key, first, data, offset + first, 4);
				if (diff != 0) {
					return diff;
				}
				diff = ByteArrayUtil.compareRegion(key, second, data, offset + second, 4);
				if (diff != 0) {
					return diff;
				}
				return ByteArrayUtil.compareRegion(key, third, data, offset + third, 4);
			};
		}
		// Common path for four fields
		int first = fieldOffsets[0];
		int second = fieldOffsets[1];
		int third = fieldOffsets[2];
		int fourth = fieldOffsets[3];
		return (key, data, offset) -> {
			int diff = ByteArrayUtil.compareRegion(key, first, data, offset + first, 4);
			if (diff != 0) {
				return diff;
			}
			diff = ByteArrayUtil.compareRegion(key, second, data, offset + second, 4);
			if (diff != 0) {
				return diff;
			}
			diff = ByteArrayUtil.compareRegion(key, third, data, offset + third, 4);
			if (diff != 0) {
				return diff;
			}
			return ByteArrayUtil.compareRegion(key, fourth, data, offset + fourth, 4);
		};
	}

	private static PatternScoreFn buildPatternScore(String fieldSeq) {
		FieldAccessor[] accessors = toAccessors(fieldSeq.toCharArray());
		if (accessors.length == 0) {
			return (subj, pred, obj, context) -> 0;
		}
		if (accessors.length == 1) {
			FieldAccessor first = accessors[0];
			return (subj, pred, obj, context) -> first.value(subj, pred, obj, context) >= 0 ? 1 : 0;
		}
		if (accessors.length == 2) {
			FieldAccessor first = accessors[0];
			FieldAccessor second = accessors[1];
			return (subj, pred, obj, context) -> {
				if (first.value(subj, pred, obj, context) < 0) {
					return 0;
				}
				return second.value(subj, pred, obj, context) < 0 ? 1 : 2;
			};
		}
		if (accessors.length == 3) {
			FieldAccessor first = accessors[0];
			FieldAccessor second = accessors[1];
			FieldAccessor third = accessors[2];
			return (subj, pred, obj, context) -> {
				if (first.value(subj, pred, obj, context) < 0) {
					return 0;
				}
				if (second.value(subj, pred, obj, context) < 0) {
					return 1;
				}
				return third.value(subj, pred, obj, context) < 0 ? 2 : 3;
			};
		}
		FieldAccessor first = accessors[0];
		FieldAccessor second = accessors[1];
		FieldAccessor third = accessors[2];
		FieldAccessor fourth = accessors[3];
		return (subj, pred, obj, context) -> {
			if (first.value(subj, pred, obj, context) < 0) {
				return 0;
			}
			if (second.value(subj, pred, obj, context) < 0) {
				return 1;
			}
			if (third.value(subj, pred, obj, context) < 0) {
				return 2;
			}
			return fourth.value(subj, pred, obj, context) < 0 ? 3 : 4;
		};
	}

	private static int[] toOffsets(char[] fieldSeq) {
		int[] offsets = new int[fieldSeq.length];
		for (int i = 0; i < fieldSeq.length; i++) {
			offsets[i] = offsetFor(fieldSeq[i]);
		}
		return offsets;
	}

	private static FieldAccessor[] toAccessors(char[] fieldSeq) {
		FieldAccessor[] accessors = new FieldAccessor[fieldSeq.length];
		for (int i = 0; i < fieldSeq.length; i++) {
			accessors[i] = accessorFor(fieldSeq[i]);
		}
		return accessors;
	}

	private static int offsetFor(char field) {
		switch (field) {
		case 's':
			return TripleStore.SUBJ_IDX;
		case 'p':
			return TripleStore.PRED_IDX;
		case 'o':
			return TripleStore.OBJ_IDX;
		case 'c':
			return TripleStore.CONTEXT_IDX;
		default:
			throw new IllegalArgumentException("invalid character '" + field + "' in field sequence: " + field);
		}
	}

	private static FieldAccessor accessorFor(char field) {
		switch (field) {
		case 's':
			return FieldAccessor.SUBJECT;
		case 'p':
			return FieldAccessor.PREDICATE;
		case 'o':
			return FieldAccessor.OBJECT;
		case 'c':
			return FieldAccessor.CONTEXT;
		default:
			throw new IllegalArgumentException("invalid character '" + field + "' in field sequence: " + field);
		}
	}

	@FunctionalInterface
	private interface FieldAccessor {
		FieldAccessor SUBJECT = (subj, pred, obj, context) -> subj;
		FieldAccessor PREDICATE = (subj, pred, obj, context) -> pred;
		FieldAccessor OBJECT = (subj, pred, obj, context) -> obj;
		FieldAccessor CONTEXT = (subj, pred, obj, context) -> context;

		int value(int subj, int pred, int obj, int context);
	}
}
