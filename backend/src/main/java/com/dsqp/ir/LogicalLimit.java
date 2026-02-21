package com.dsqp.ir;

import java.util.Set;

public record LogicalLimit(LogicalPlan input, int limit, int offset) implements LogicalPlan {
    @Override
    public Set<String> referencedTables() {
        return input.referencedTables();
    }
}
