package com.dsqp.ir;

import java.util.List;

public record PhysicalSort(PhysicalPlan input, List<SortKey> sortKeys) implements PhysicalPlan {
    @Override
    public java.util.Set<String> involvedBackends() {
        return input.involvedBackends();
    }
}
