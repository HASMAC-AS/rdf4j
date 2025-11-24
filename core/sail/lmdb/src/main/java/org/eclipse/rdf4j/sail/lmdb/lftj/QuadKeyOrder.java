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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Describes the positional order of quad components inside an index key.
 */
public enum QuadKeyOrder {
	CSOP(Slot.C, Slot.S, Slot.O, Slot.P),
	CSPO(Slot.C, Slot.S, Slot.P, Slot.O),
	COSP(Slot.C, Slot.O, Slot.S, Slot.P),
	COPS(Slot.C, Slot.O, Slot.P, Slot.S),
	CPSO(Slot.C, Slot.P, Slot.S, Slot.O),
	CPOS(Slot.C, Slot.P, Slot.O, Slot.S),
	OSCP(Slot.O, Slot.S, Slot.C, Slot.P),
	OSPC(Slot.O, Slot.S, Slot.P, Slot.C),
	OCSP(Slot.O, Slot.C, Slot.S, Slot.P),
	OCPS(Slot.O, Slot.C, Slot.P, Slot.S),
	OPSC(Slot.O, Slot.P, Slot.S, Slot.C),
	OPCS(Slot.O, Slot.P, Slot.C, Slot.S),
	PSOC(Slot.P, Slot.S, Slot.O, Slot.C),
	PSCO(Slot.P, Slot.S, Slot.C, Slot.O),
	POSC(Slot.P, Slot.O, Slot.S, Slot.C),
	POCS(Slot.P, Slot.O, Slot.C, Slot.S),
	PCSO(Slot.P, Slot.C, Slot.S, Slot.O),
	PCOS(Slot.P, Slot.C, Slot.O, Slot.S),
	SPOC(Slot.S, Slot.P, Slot.O, Slot.C),
	SPCO(Slot.S, Slot.P, Slot.C, Slot.O),
	SOPC(Slot.S, Slot.O, Slot.P, Slot.C),
	SOCP(Slot.S, Slot.O, Slot.C, Slot.P),
	SCPO(Slot.S, Slot.C, Slot.P, Slot.O),
	SCOP(Slot.S, Slot.C, Slot.O, Slot.P);

	private static final int KEY_LENGTH = 4;
	private static final Map<String, QuadKeyOrder> BY_SEQUENCE;

	private final Slot[] positions;
	private final String fieldSequence;
	private final QuadKeyEncoding.QuadKeyDecoder decoder;
	private final QuadKeyEncoding.QuadKeyEncoder encoder;

	static {
		Map<String, QuadKeyOrder> bySequence = new HashMap<>();
		for (QuadKeyOrder order : values()) {
			bySequence.put(order.fieldSequence, order);
		}
		BY_SEQUENCE = Map.copyOf(bySequence);
	}

	QuadKeyOrder(Slot... positions) {
		validatePositions(positions);
		this.positions = positions.clone();
		this.fieldSequence = buildFieldSequence(positions);
		this.decoder = QuadKeyEncoding.decoderFor(fieldSequence);
		this.encoder = QuadKeyEncoding.encoderFor(fieldSequence);
	}

	public static QuadKeyOrder of(Slot... positions) {
		Objects.requireNonNull(positions, "positions");
		validatePositions(positions);
		String sequence = buildFieldSequence(positions);
		QuadKeyOrder order = BY_SEQUENCE.get(sequence);
		if (order == null) {
			throw new IllegalArgumentException("QuadKeyOrder must contain each slot exactly once");
		}
		return order;
	}

	public static QuadKeyOrder fromFieldSequence(String sequence) {
		Objects.requireNonNull(sequence, "sequence");
		if (sequence.length() != KEY_LENGTH) {
			throw new IllegalArgumentException("Field sequence must be four characters: " + sequence);
		}
		String normalized = sequence.toLowerCase(Locale.ROOT);
		QuadKeyOrder order = BY_SEQUENCE.get(normalized);
		if (order != null) {
			return order;
		}
		EnumSet<Slot> unique = EnumSet.noneOf(Slot.class);
		for (int i = 0; i < KEY_LENGTH; i++) {
			Slot slot = toSlot(normalized.charAt(i));
			if (!unique.add(slot)) {
				throw new IllegalArgumentException("Field sequence must contain each slot exactly once: " + sequence);
			}
		}
		throw new IllegalArgumentException("Unknown field in sequence: " + sequence);
	}

	private static Slot toSlot(char ch) {
		switch (Character.toLowerCase(ch)) {
		case 's':
			return Slot.S;
		case 'p':
			return Slot.P;
		case 'o':
			return Slot.O;
		case 'c':
			return Slot.C;
		default:
			throw new IllegalArgumentException("Unknown field in sequence: " + ch);
		}
	}

	public List<Slot> positions() {
		return List.copyOf(Arrays.asList(positions));
	}

	public Slot positionAt(int index) {
		return positions[index];
	}

	public int indexOf(Slot slot) {
		for (int i = 0; i < positions.length; i++) {
			if (positions[i] == slot) {
				return i;
			}
		}
		return -1;
	}

	public String fieldSequence() {
		return fieldSequence;
	}

	QuadKeyEncoding.QuadKeyDecoder decoder() {
		return decoder;
	}

	QuadKeyEncoding.QuadKeyEncoder encoder() {
		return encoder;
	}

	private static void validatePositions(Slot[] positions) {
		if (positions.length != KEY_LENGTH) {
			throw new IllegalArgumentException("QuadKeyOrder must contain exactly four slots");
		}
		EnumSet<Slot> unique = EnumSet.noneOf(Slot.class);
		unique.addAll(Arrays.asList(positions));
		if (unique.size() != KEY_LENGTH) {
			throw new IllegalArgumentException("QuadKeyOrder must contain each slot exactly once");
		}
	}

	private static String buildFieldSequence(Slot[] positions) {
		StringBuilder builder = new StringBuilder(KEY_LENGTH);
		for (Slot position : positions) {
			switch (position) {
			case S:
				builder.append('s');
				break;
			case P:
				builder.append('p');
				break;
			case O:
				builder.append('o');
				break;
			case C:
				builder.append('c');
				break;
			default:
				throw new IllegalStateException("Unexpected slot: " + position);
			}
		}
		return builder.toString();
	}

	public void printOrder() {
		System.out.print("Order: ");
		for (Slot slot : positions) {
			System.out.print(slot + " ");
		}
		System.out.println();
	}

	public void printBound(long s, long p, long o, long c) {
		for (Slot position : positions) {
			switch (position) {
			case S:
				System.out.print("S:");
				if (s > 0) {
					System.out.print("Bound");
				} else {
					System.out.print("Unbound");
				}
				System.out.print("  ");
				break;
			case P:
				System.out.print("P:");
				if (p > 0) {
					System.out.print("Bound");
				} else {
					System.out.print("Unbound");
				}
				System.out.print("  ");

				break;
			case O:
				System.out.print("O:");
				if (o > 0) {
					System.out.print("Bound");
				} else {
					System.out.print("Unbound");
				}
				System.out.print("  ");

				break;
			case C:
				System.out.print("C:");
				if (c > 0) {
					System.out.print("Bound");
				} else {
					System.out.print("Unbound");
				}
				System.out.print("  ");

				break;
			}
		}
		System.out.println();

	}

	public boolean isOptimal(long s, long p, long o, long c) {
		boolean unboundFound = false;
		for (Slot position : positions) {
			// if the value for the position is <= 0, it is unbound, e.g. for position S: s <= 0
			boolean isBound;
			switch (position) {
			case S:
				isBound = s > 0;
				break;
			case P:
				isBound = p > 0;
				break;
			case O:
				isBound = o > 0;
				break;
			case C:
				isBound = c > 0;
				break;
			default:
				throw new IllegalStateException("Unexpected slot: " + position);
			}

			// Once we have encountered an unbound slot, any later bound slot means the order
			// is not optimal (bound-after-unbound).
			if (unboundFound && isBound) {
				return false;
			}
			if (!isBound) {
				unboundFound = true;
			}

		}
		return true;

	}
}
