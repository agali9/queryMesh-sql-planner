package com.dsqp.ir;

import java.util.Set;

public sealed interface PhysicalPlan permits
        PhysicalBackendScan, PhysicalFilter, PhysicalProject, PhysicalHashJoin,
        PhysicalMergeJoin, PhysicalUnionAll, PhysicalSort, PhysicalLimit,
        PhysicalAggregate {

    Set<String> involvedBackends();
}
