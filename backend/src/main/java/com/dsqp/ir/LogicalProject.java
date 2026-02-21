package com.dsqp.ir;

import java.util.List;
import java.util.Set;

public record LogicalProject(LogicalPlan input, List<ProjectionExpr> projections) implements LogicalPlan {
    @Override
    public Set<String> referencedTables() {
        return input.referencedTables();
    }
}
