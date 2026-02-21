package com.dsqp.ir;

public record PhysicalFilter(PhysicalPlan input, String predicate) implements PhysicalPlan {
    @Override
    public java.util.Set<String> involvedBackends() {
        return input.involvedBackends();
    }
}
