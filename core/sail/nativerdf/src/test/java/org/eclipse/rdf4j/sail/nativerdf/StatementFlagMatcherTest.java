package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class StatementFlagMatcherTest {

	@Test
	void explicitFilterMatchesLegacyLogic() {
		StatementFlagMatcher matcher = StatementFlagMatcher.explicitFilter();

		IntStream.range(0, 256).forEach(i -> {
			byte flags = (byte) i;
			boolean expected = legacyExplicit(flags);
			assertThat(matcher.matches(flags)).as("flags=%s", Integer.toBinaryString(i)).isEqualTo(expected);
		});
	}

	@Test
	void implicitFilterMatchesLegacyLogic() {
		StatementFlagMatcher matcher = StatementFlagMatcher.implicitFilter();

		IntStream.range(0, 256).forEach(i -> {
			byte flags = (byte) i;
			boolean expected = legacyImplicit(flags);
			assertThat(matcher.matches(flags)).as("flags=%s", Integer.toBinaryString(i)).isEqualTo(expected);
		});
	}

	private static boolean legacyExplicit(byte flags) {
		boolean explicit = (flags & TripleStore.EXPLICIT_FLAG) != 0;
		boolean toggled = (flags & TripleStore.TOGGLE_EXPLICIT_FLAG) != 0;
		return explicit != toggled;
	}

	private static boolean legacyImplicit(byte flags) {
		boolean explicit = (flags & TripleStore.EXPLICIT_FLAG) != 0;
		return !explicit;
	}
}
