package com.dsqp.ir;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record PhysicalHashJoin(
        PhysicalPlan buildSide,
        PhysicalPlan probeSide,
        List<String> buildKeys,
        List<String> probeKeys,
        JoinType joinType
) implements PhysicalPlan {
    @Override
    public Set<String> involvedBackends() {
        var backends = new HashSet<>(buildSide.involvedBackends());
        backends.addAll(probeSide.involvedBackends());
        return Set.copyOf(backends);
    }
}
