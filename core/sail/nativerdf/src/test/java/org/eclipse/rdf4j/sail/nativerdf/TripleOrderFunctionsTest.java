package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.sail.nativerdf.TripleOrderFunctions.CompareFn;
import org.eclipse.rdf4j.sail.nativerdf.TripleOrderFunctions.PatternScoreFn;
import org.junit.jupiter.api.Test;

class TripleOrderFunctionsTest {

	private static final List<String> FIELD_SEQUENCES = Stream.of("spoc", "sopc", "psoc", "posc", "ospc", "opsc")
			.collect(Collectors.toList());

	@Test
	void comparatorMatchesLegacyImplementation() {
		Random random = new Random(1234L);

		for (String fieldSeq : FIELD_SEQUENCES) {
			CompareFn comparator = TripleOrderFunctions.comparatorFor(fieldSeq.toCharArray());

			for (int i = 0; i < 512; i++) {
				byte[] key = randomRecord(random);
				byte[] value = randomRecord(random);

				int expected = legacyCompare(fieldSeq, key, value, 0);
				int actual = comparator.compare(key, value, 0);

				if (expected == 0) {
					// simulate different order within equals
					Arrays.fill(value, (byte) 0);
					actual = comparator.compare(key, value, 0);
					expected = legacyCompare(fieldSeq, key, value, 0);
				}

				assertThat(Integer.signum(actual)).isEqualTo(Integer.signum(expected));
			}
		}
	}

	@Test
	void patternScoreMatchesLegacyImplementation() {
		PatternScoreFn calculator = TripleOrderFunctions.patternScoreFor("spoc".toCharArray());

		for (int subj : new int[] { -1, 0, 7 }) {
			for (int pred : new int[] { -1, 0, 9 }) {
				for (int obj : new int[] { -1, 1 }) {
					for (int context : new int[] { -1, 2 }) {
						int expected = legacyScore("spoc", subj, pred, obj, context);
						assertThat(calculator.score(subj, pred, obj, context)).isEqualTo(expected);
					}
				}
			}
		}
	}

	private static byte[] randomRecord(Random random) {
		byte[] data = new byte[TripleStore.RECORD_LENGTH];
		random.nextBytes(data);
		return data;
	}

	private static int legacyCompare(String fieldSeq, byte[] key, byte[] data, int offset) {
		for (char field : fieldSeq.toCharArray()) {
			int fieldIdx;
			switch (field) {
			case 's':
				fieldIdx = TripleStore.SUBJ_IDX;
				break;
			case 'p':
				fieldIdx = TripleStore.PRED_IDX;
				break;
			case 'o':
				fieldIdx = TripleStore.OBJ_IDX;
				break;
			case 'c':
				fieldIdx = TripleStore.CONTEXT_IDX;
				break;
			default:
				throw new IllegalArgumentException();
			}
			int diff = ByteArrayUtil.compareRegion(key, fieldIdx, data, offset + fieldIdx, 4);
			if (diff != 0) {
				return diff;
			}
		}
		return 0;
	}

	private static int legacyScore(String fieldSeq, int subj, int pred, int obj, int context) {
		int score = 0;
		for (char field : fieldSeq.toCharArray()) {
			switch (field) {
			case 's':
				if (subj >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			case 'p':
				if (pred >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			case 'o':
				if (obj >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			case 'c':
				if (context >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			default:
				throw new IllegalArgumentException();
			}
		}
		return score;
	}
}
