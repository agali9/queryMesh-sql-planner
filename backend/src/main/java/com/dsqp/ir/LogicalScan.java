package com.dsqp.ir;

import java.util.Set;

public record LogicalScan(String tableName, String alias) implements LogicalPlan {
    @Override
    public Set<String> referencedTables() {
        return Set.of(tableName);
    }
}
