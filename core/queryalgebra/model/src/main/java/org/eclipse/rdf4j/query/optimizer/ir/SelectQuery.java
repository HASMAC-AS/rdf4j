package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** SELECT query logical representation. */
public final class SelectQuery implements QueryIr {

	private final boolean distinct;
	private final List<ProjectionElement> projection;
	private final Pattern where;
	private final List<OrderCondition> orderBy;
	private final Long limit;
	private final Long offset;
	private final List<Var> groupBy;
	private final Expr having;

	public SelectQuery(boolean distinct, List<ProjectionElement> projection, Pattern where,
			List<OrderCondition> orderBy, Long limit, Long offset, List<Var> groupBy, Expr having) {
		this.distinct = distinct;
		this.projection = Collections.unmodifiableList(Objects.requireNonNull(projection, "projection"));
		this.where = Objects.requireNonNull(where, "where");
		this.orderBy = Collections.unmodifiableList(Objects.requireNonNull(orderBy, "orderBy"));
		this.limit = limit;
		this.offset = offset;
		this.groupBy = Collections.unmodifiableList(Objects.requireNonNull(groupBy, "groupBy"));
		this.having = having;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public List<ProjectionElement> getProjection() {
		return projection;
	}

	public Pattern getWhere() {
		return where;
	}

	public List<OrderCondition> getOrderBy() {
		return orderBy;
	}

	public Long getLimit() {
		return limit;
	}

	public Long getOffset() {
		return offset;
	}

	public List<Var> getGroupBy() {
		return groupBy;
	}

	public Expr getHaving() {
		return having;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SelectQuery)) {
			return false;
		}
		SelectQuery that = (SelectQuery) o;
		return distinct == that.distinct
				&& projection.equals(that.projection)
				&& where.equals(that.where)
				&& orderBy.equals(that.orderBy)
				&& Objects.equals(limit, that.limit)
				&& Objects.equals(offset, that.offset)
				&& groupBy.equals(that.groupBy)
				&& Objects.equals(having, that.having);
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode(distinct);
		result = 31 * result + projection.hashCode();
		result = 31 * result + where.hashCode();
		result = 31 * result + orderBy.hashCode();
		result = 31 * result + (limit != null ? limit.hashCode() : 0);
		result = 31 * result + (offset != null ? offset.hashCode() : 0);
		result = 31 * result + groupBy.hashCode();
		result = 31 * result + (having != null ? having.hashCode() : 0);
		return result;
	}
}
