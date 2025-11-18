package org.eclipse.rdf4j.sail.nativerdf.btree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.junit.jupiter.api.Test;

class RangeIteratorValueMatcherTest {

	private static final int RECORD_LENGTH = 17;
	private static final int SUBJECT_OFFSET = 0;
	private static final int PREDICATE_OFFSET = 4;
	private static final int OBJECT_OFFSET = 8;
	private static final int CONTEXT_OFFSET = 12;
	private static final int FLAG_OFFSET = 16;

	private static final int SUBJECT_VALUE = 11;
	private static final int PREDICATE_VALUE = 22;
	private static final int OBJECT_VALUE = 33;
	private static final int CONTEXT_VALUE = 44;

	private static final int ADDED_FLAG = 0x2;
	private static final int EXPLICIT_FLAG = 0x1;

	@Test
	void matcherAgreesWithByteArrayUtilForCommonPatterns() {
		byte[] baseKey = record(SUBJECT_VALUE, PREDICATE_VALUE, OBJECT_VALUE, CONTEXT_VALUE,
				ADDED_FLAG | EXPLICIT_FLAG);

		List<byte[]> candidateValues = generateCandidateValues();

		int[] flagMasks = new int[] { 0, 0xFF, ADDED_FLAG, ADDED_FLAG | EXPLICIT_FLAG };

		for (int mask = 0; mask < 16; mask++) {
			int maskBits = mask;
			boolean subjMatch = (mask & 0b0001) != 0;
			boolean predMatch = (mask & 0b0010) != 0;
			boolean objMatch = (mask & 0b0100) != 0;
			boolean ctxMatch = (mask & 0b1000) != 0;

			for (int flagMask : flagMasks) {
				byte[] searchMask = createMask(subjMatch, predMatch, objMatch, ctxMatch, flagMask);
				RangeIterator.ValueMatcher matcher = RangeIterator.ValueMatcher.create(baseKey, searchMask);

				for (byte[] value : candidateValues) {
					boolean expected = ByteArrayUtil.matchesPattern(value, searchMask, baseKey);
					boolean actual = matcher.matches(value);
					assertEquals(expected, actual,
							() -> String.format("Mismatch for mask=%s flagsMask=%02X value=%s", binary(maskBits),
									flagMask, describe(value)));
				}
			}
		}
	}

	private static String binary(int mask) {
		return String.format("%4s", Integer.toBinaryString(mask)).replace(' ', '0');
	}

	private static String describe(byte[] value) {
		int subj = ByteArrayUtil.getInt(value, SUBJECT_OFFSET);
		int pred = ByteArrayUtil.getInt(value, PREDICATE_OFFSET);
		int obj = ByteArrayUtil.getInt(value, OBJECT_OFFSET);
		int ctx = ByteArrayUtil.getInt(value, CONTEXT_OFFSET);
		int flag = Byte.toUnsignedInt(value[FLAG_OFFSET]);
		return String.format("[%d,%d,%d,%d|%02X]", subj, pred, obj, ctx, flag);
	}

	private static byte[] record(int subj, int pred, int obj, int context, int flags) {
		byte[] value = new byte[RECORD_LENGTH];
		ByteArrayUtil.putInt(subj, value, SUBJECT_OFFSET);
		ByteArrayUtil.putInt(pred, value, PREDICATE_OFFSET);
		ByteArrayUtil.putInt(obj, value, OBJECT_OFFSET);
		ByteArrayUtil.putInt(context, value, CONTEXT_OFFSET);
		value[FLAG_OFFSET] = (byte) flags;
		return value;
	}

	private static List<byte[]> generateCandidateValues() {
		int[] ids = new int[] { 0, 1, 5, 42, SUBJECT_VALUE, PREDICATE_VALUE, OBJECT_VALUE, CONTEXT_VALUE };
		int[] contexts = new int[] { 0, 7, CONTEXT_VALUE };
		int[] flags = new int[] { 0, ADDED_FLAG, EXPLICIT_FLAG, ADDED_FLAG | EXPLICIT_FLAG, 0xFF };

		List<byte[]> values = new ArrayList<>();
		for (int s : ids) {
			for (int p : ids) {
				for (int o : ids) {
					for (int c : contexts) {
						for (int f : flags) {
							values.add(record(s, p, o, c, f));
						}
					}
				}
			}
		}
		return values;
	}

	private static byte[] createMask(boolean subj, boolean pred, boolean obj, boolean ctx, int flagMask) {
		byte[] mask = new byte[RECORD_LENGTH];
		if (subj) {
			fillInt(mask, SUBJECT_OFFSET);
		}
		if (pred) {
			fillInt(mask, PREDICATE_OFFSET);
		}
		if (obj) {
			fillInt(mask, OBJECT_OFFSET);
		}
		if (ctx) {
			fillInt(mask, CONTEXT_OFFSET);
		}
		mask[FLAG_OFFSET] = (byte) flagMask;
		return mask;
	}

	private static void fillInt(byte[] target, int offset) {
		ByteArrayUtil.putInt(0xFFFFFFFF, target, offset);
	}
}
