package com.dsqp.ir;

import java.util.List;
import java.util.Set;

public record PhysicalBackendScan(
        String backendId,
        String rewrittenSql,
        List<String> outputColumns
) implements PhysicalPlan {
    @Override
    public Set<String> involvedBackends() {
        return Set.of(backendId);
    }
}
