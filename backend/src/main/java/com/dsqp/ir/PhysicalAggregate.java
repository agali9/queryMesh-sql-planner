package com.dsqp.ir;

import java.util.List;

public record PhysicalAggregate(
        PhysicalPlan input,
        List<String> groupByColumns,
        List<AggregateExpr> aggregates
) implements PhysicalPlan {
    @Override
    public java.util.Set<String> involvedBackends() {
        return input.involvedBackends();
    }
}
