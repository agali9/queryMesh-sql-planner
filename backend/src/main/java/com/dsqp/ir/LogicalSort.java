package com.dsqp.ir;

import java.util.List;
import java.util.Set;

public record LogicalSort(LogicalPlan input, List<SortKey> sortKeys) implements LogicalPlan {
    @Override
    public Set<String> referencedTables() {
        return input.referencedTables();
    }
}
