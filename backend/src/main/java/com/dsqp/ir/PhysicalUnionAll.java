package com.dsqp.ir;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record PhysicalUnionAll(List<PhysicalPlan> inputs) implements PhysicalPlan {
    @Override
    public Set<String> involvedBackends() {
        return inputs.stream()
                .flatMap(p -> p.involvedBackends().stream())
                .collect(Collectors.toSet());
    }
}
