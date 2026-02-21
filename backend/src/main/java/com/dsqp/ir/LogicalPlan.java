package com.dsqp.ir;

import java.util.List;
import java.util.Set;

public sealed interface LogicalPlan permits
        LogicalScan, LogicalFilter, LogicalProject, LogicalJoin,
        LogicalAggregate, LogicalSort, LogicalLimit {

    Set<String> referencedTables();
}
