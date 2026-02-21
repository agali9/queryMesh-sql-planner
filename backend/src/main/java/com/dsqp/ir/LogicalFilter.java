package com.dsqp.ir;

import java.util.Set;

public record LogicalFilter(LogicalPlan input, String predicate) implements LogicalPlan {
    @Override
    public Set<String> referencedTables() {
        return input.referencedTables();
    }
}
