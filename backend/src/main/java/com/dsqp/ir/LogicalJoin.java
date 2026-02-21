package com.dsqp.ir;

import java.util.HashSet;
import java.util.Set;

public record LogicalJoin(
        LogicalPlan left,
        LogicalPlan right,
        JoinType joinType,
        String condition
) implements LogicalPlan {
    @Override
    public Set<String> referencedTables() {
        var tables = new HashSet<>(left.referencedTables());
        tables.addAll(right.referencedTables());
        return Set.copyOf(tables);
    }
}
