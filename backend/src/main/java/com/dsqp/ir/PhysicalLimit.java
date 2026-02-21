package com.dsqp.ir;

public record PhysicalLimit(PhysicalPlan input, int limit, int offset) implements PhysicalPlan {
    @Override
    public java.util.Set<String> involvedBackends() {
        return input.involvedBackends();
    }
}
