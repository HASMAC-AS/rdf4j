package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** SERVICE pattern. */
public final class ServicePattern implements Pattern {

	private final Term serviceIri;
	private final Pattern inner;
	private final boolean silent;

	public ServicePattern(Term serviceIri, Pattern inner, boolean silent) {
		this.serviceIri = Objects.requireNonNull(serviceIri, "serviceIri");
		this.inner = Objects.requireNonNull(inner, "inner");
		this.silent = silent;
	}

	public Term getServiceIri() {
		return serviceIri;
	}

	public Pattern getInner() {
		return inner;
	}

	public boolean isSilent() {
		return silent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ServicePattern)) {
			return false;
		}
		ServicePattern that = (ServicePattern) o;
		return silent == that.silent
				&& serviceIri.equals(that.serviceIri)
				&& inner.equals(that.inner);
	}

	@Override
	public int hashCode() {
		int result = serviceIri.hashCode();
		result = 31 * result + inner.hashCode();
		result = 31 * result + (silent ? 1 : 0);
		return result;
	}
}
