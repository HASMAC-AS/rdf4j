package org.eclipse.rdf4j.query.optimizer.ir;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class SemanticAnalyzerTest {

	private final Var s = new Var("s");
	private final Var p = new Var("p");
	private final Var o = new Var("o");
	private final Var x = new Var("x");
	private final Var y = new Var("y");
	private final Var z = new Var("z");

	@Test
	void bgpVarsAreCertain() {
		Bgp bgp = new Bgp(Collections.singletonList(new TriplePattern(s, p, o)));

		SemanticAnalyzer analyzer = new SemanticAnalyzer();
		AnalysisResult result = analyzer.analyze(bgp);

		SemanticInfo info = result.getInfo(bgp);
		assertThat(info.getVars()).containsExactlyInAnyOrder(s, p, o);
		assertThat(info.getCertainVars()).containsExactlyInAnyOrder(s, p, o);
		assertThat(info.isMonotone()).isTrue();
		assertThat(info.isWellDesigned()).isTrue();
	}

	@Test
	void certainVarsFollowLeftJoinRules() {
		Bgp left = new Bgp(Collections.singletonList(new TriplePattern(s, p, x)));
		Bgp right = new Bgp(Collections.singletonList(new TriplePattern(x, p, y)));
		LeftJoin optional = new LeftJoin(left, right, null);

		SemanticAnalyzer analyzer = new SemanticAnalyzer();
		AnalysisResult result = analyzer.analyze(optional);

		SemanticInfo info = result.getInfo(optional);
		assertThat(info.getCertainVars()).containsExactlyInAnyOrder(s, p, x);
		assertThat(info.getCertainVars()).doesNotContain(y);
	}

	@Test
	void unionIntersectsCertainVars() {
		Bgp left = new Bgp(Collections.singletonList(new TriplePattern(s, p, x)));
		Bgp right = new Bgp(Collections.singletonList(new TriplePattern(s, p, y)));
		UnionPattern union = new UnionPattern(Arrays.asList(left, right));

		SemanticAnalyzer analyzer = new SemanticAnalyzer();
		AnalysisResult result = analyzer.analyze(union);

		SemanticInfo info = result.getInfo(union);
		assertThat(info.getVars()).containsExactlyInAnyOrder(s, p, x, y);
		assertThat(info.getCertainVars()).containsExactlyInAnyOrder(s, p);
	}

	@Test
	void valuesTreatUndefinedAsUncertain() {
		ValuesPattern values = new ValuesPattern(
				Collections.singletonList(x),
				Arrays.asList(
						Collections.singletonList(TermOrUndef.undef()),
						Collections.singletonList(TermOrUndef.of(new Iri("urn:v1")))));

		SemanticAnalyzer analyzer = new SemanticAnalyzer();
		AnalysisResult result = analyzer.analyze(values);

		SemanticInfo info = result.getInfo(values);
		assertThat(info.getVars()).containsExactly(x);
		assertThat(info.getCertainVars()).isEmpty();
	}

	@Test
	void monotonicityReflectsNegation() {
		MinusPattern minus = new MinusPattern(
				new Bgp(Collections.singletonList(new TriplePattern(s, p, o))),
				new Bgp(Collections.singletonList(new TriplePattern(x, p, y))));

		SemanticAnalyzer analyzer = new SemanticAnalyzer();
		AnalysisResult result = analyzer.analyze(minus);

		assertThat(result.getInfo(minus).isMonotone()).isFalse();
	}

	@Test
	void optionalWellDesignednessIsTracked() {
		// well-designed OPTIONAL: right-side var also in left side when used outside
		LeftJoin wellDesigned = new LeftJoin(
				new Bgp(Collections.singletonList(new TriplePattern(s, p, x))),
				new Bgp(Collections.singletonList(new TriplePattern(x, p, y))),
				null);
		Pattern rootWellDesigned = new Join(
				wellDesigned,
				new Bgp(Collections.singletonList(new TriplePattern(y, p, o))));

		SemanticAnalyzer analyzer = new SemanticAnalyzer();
		AnalysisResult wellDesignedResult = analyzer.analyze(rootWellDesigned);
		assertThat(wellDesignedResult.getInfo(wellDesigned).isWellDesigned()).isTrue();
		assertThat(wellDesignedResult.getInfo(rootWellDesigned).isWellDesigned()).isTrue();

		// not well-designed: right-side var used outside but not provided by left side
		LeftJoin notWellDesigned = new LeftJoin(
				new Bgp(Collections.singletonList(new TriplePattern(s, p, x))),
				new Bgp(Collections.singletonList(new TriplePattern(y, p, z))),
				null);
		Pattern rootNotWellDesigned = new Join(
				notWellDesigned,
				new Bgp(Collections.singletonList(new TriplePattern(z, p, o))));

		AnalysisResult notWellDesignedResult = analyzer.analyze(rootNotWellDesigned);
		assertThat(notWellDesignedResult.getInfo(notWellDesigned).isWellDesigned()).isFalse();
		assertThat(notWellDesignedResult.getInfo(rootNotWellDesigned).isWellDesigned()).isFalse();
	}
}
