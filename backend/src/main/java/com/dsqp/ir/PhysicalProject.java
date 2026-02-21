package com.dsqp.ir;

import java.util.List;

public record PhysicalProject(PhysicalPlan input, List<ProjectionExpr> projections) implements PhysicalPlan {
    @Override
    public java.util.Set<String> involvedBackends() {
        return input.involvedBackends();
    }
}
