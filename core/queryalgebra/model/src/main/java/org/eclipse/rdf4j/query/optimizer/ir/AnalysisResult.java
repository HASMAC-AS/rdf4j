package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Map;
import java.util.Objects;

/** Holds semantic info results per pattern. */
public final class AnalysisResult {

	private final Map<Pattern, SemanticInfo> infoByPattern;

	AnalysisResult(Map<Pattern, SemanticInfo> infoByPattern) {
		this.infoByPattern = infoByPattern;
	}

	public SemanticInfo getInfo(Pattern pattern) {
		SemanticInfo info = infoByPattern.get(pattern);
		return Objects.requireNonNull(info, "No SemanticInfo for pattern");
	}
}
