package com.dsqp.service;

import com.dsqp.execution.ExecutionEngine;
import com.dsqp.failure.FailureMode;
import com.dsqp.parser.SqlParser;
import com.dsqp.planner.QueryPlanner;
import org.springframework.stereotype.Service;

@Service
public class QueryService {

    private final SqlParser parser;
    private final QueryPlanner planner;
    private final ExecutionEngine executionEngine;

    public QueryService(SqlParser parser, QueryPlanner planner, ExecutionEngine executionEngine) {
        this.parser = parser;
        this.planner = planner;
        this.executionEngine = executionEngine;
    }

    public ExecutionEngine.QueryResult execute(String sql) {
        return execute(sql, null);
    }

    public ExecutionEngine.QueryResult execute(String sql, FailureMode failureMode) {
        var parsed = parser.parse(sql);
        var planned = planner.plan(parsed.logicalPlan());
        if (failureMode != null) {
            return executionEngine.execute(planned, failureMode);
        }
        return executionEngine.execute(planned);
    }

    public QueryPlanExplanation explain(String sql) {
        var parsed = parser.parse(sql);
        var planned = planner.plan(parsed.logicalPlan());
        return new QueryPlanExplanation(
                sql,
                parsed.logicalPlan().toString(),
                planned.physical().toString(),
                planned.involvedBackends()
        );
    }

    public record QueryPlanExplanation(
            String sql,
            String logicalPlan,
            String physicalPlan,
            java.util.Set<String> involvedBackends
    ) {}
}
