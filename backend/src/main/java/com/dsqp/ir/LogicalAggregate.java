package com.dsqp.ir;

import java.util.List;
import java.util.Set;

public record LogicalAggregate(
        LogicalPlan input,
        List<String> groupByColumns,
        List<AggregateExpr> aggregates
) implements LogicalPlan {
    @Override
    public Set<String> referencedTables() {
        return input.referencedTables();
    }
}
