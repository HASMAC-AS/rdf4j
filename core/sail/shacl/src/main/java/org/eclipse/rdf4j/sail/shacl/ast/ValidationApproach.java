package org.eclipse.rdf4j.sail.shacl.ast;

public enum ValidationApproach {

	Transactional,
	SPARQL;

	public static final ValidationApproach MOST_COMPATIBLE = Transactional;

	public static ValidationApproach reducePreferred(ValidationApproach a, ValidationApproach b) {
		if (a == SPARQL) {
			return a;
		}
		if (b == SPARQL) {
			return b;
		}

		return a;
	}

	public static ValidationApproach reduceCompatible(ValidationApproach a, ValidationApproach b) {
		if (a == SPARQL) {
			return a;
		}
		if (b == SPARQL) {
			return b;
		}

		return a;
	}
}
