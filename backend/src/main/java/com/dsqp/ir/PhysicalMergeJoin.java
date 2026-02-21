package com.dsqp.ir;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record PhysicalMergeJoin(
        PhysicalPlan left,
        PhysicalPlan right,
        List<String> leftKeys,
        List<String> rightKeys,
        JoinType joinType
) implements PhysicalPlan {
    @Override
    public Set<String> involvedBackends() {
        var backends = new HashSet<>(left.involvedBackends());
        backends.addAll(right.involvedBackends());
        return Set.copyOf(backends);
    }
}
