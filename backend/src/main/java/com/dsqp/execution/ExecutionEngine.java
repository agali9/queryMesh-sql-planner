package com.dsqp.execution;

import com.dsqp.config.DsqpProperties;
import com.dsqp.execution.backend.BackendConnectionPool;
import com.dsqp.execution.iterator.*;
import com.dsqp.failure.FailureHandler;
import com.dsqp.failure.FailureMode;
import com.dsqp.failure.PartialFailureCollector;
import com.dsqp.failure.PartialFailureReport;
import com.dsqp.ir.*;
import com.dsqp.model.Row;
import com.dsqp.model.Schema;
import com.dsqp.planner.QueryPlanner;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates pipelined query execution using Volcano-style iterators.
 */
@Service
public class ExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEngine.class);

    private final BackendConnectionPool connectionPool;
    private final FailureHandler failureHandler;
    private final int fetchSize;
    private final Timer queryTimer;

    public ExecutionEngine(
            BackendConnectionPool connectionPool,
            FailureHandler failureHandler,
            DsqpProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.connectionPool = connectionPool;
        this.failureHandler = failureHandler;
        this.fetchSize = properties.execution().fetchSize();
        this.queryTimer = Timer.builder("dsqp.query.execution")
                .description("End-to-end query execution time")
                .register(meterRegistry);
    }

    public QueryResult execute(QueryPlanner.PlannedQuery planned) {
        return execute(planned, failureHandler.getDefaultMode());
    }

    public QueryResult execute(QueryPlanner.PlannedQuery planned, FailureMode failureMode) {
        long start = System.nanoTime();
        PartialFailureCollector failureCollector = new PartialFailureCollector();

        try (RowIterator iterator = buildIterator(planned.physical(), failureCollector)) {
            List<Map<String, Object>> rows = new ArrayList<>();
            Schema schema = iterator.schema();
            Row row;
            while ((row = iterator.next()) != null) {
                rows.add(new LinkedHashMap<>(row.values()));
            }

            PartialFailureReport report = failureCollector.buildReport(failureMode);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            queryTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);

            log.info("Query completed in {}ms, {} rows, backends={}, degraded={}",
                    elapsedMs, rows.size(), planned.involvedBackends(), report.queryDegraded());

            return new QueryResult(
                    rows,
                    schema.columnNames(),
                    planned.involvedBackends(),
                    elapsedMs,
                    report
            );
        } catch (Exception e) {
            queryTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            throw e;
        }
    }

    public StreamingQueryResult executeStreaming(QueryPlanner.PlannedQuery planned) {
        PartialFailureCollector failureCollector = new PartialFailureCollector();
        RowIterator iterator = buildIterator(planned.physical(), failureCollector);
        return new StreamingQueryResult(iterator, failureCollector, failureHandler.getDefaultMode());
    }

    private RowIterator buildIterator(PhysicalPlan plan, PartialFailureCollector failureCollector) {
        return switch (plan) {
            case PhysicalBackendScan scan -> new BackendScanIterator(
                    scan.backendId(), scan.rewrittenSql(), scan.outputColumns(),
                    connectionPool, fetchSize, failureCollector);
            case PhysicalFilter filter -> new FilterIterator(
                    buildIterator(filter.input(), failureCollector), filter.predicate());
            case PhysicalProject project -> new ProjectIterator(
                    buildIterator(project.input(), failureCollector),
                    project.projections().stream().map(ProjectionExpr::expression).toList(),
                    project.projections().stream().map(ProjectionExpr::alias).toList());
            case PhysicalHashJoin join -> new HashJoinIterator(
                    buildIterator(join.buildSide(), failureCollector),
                    buildIterator(join.probeSide(), failureCollector),
                    join.buildKeys(), join.probeKeys(), join.joinType());
            case PhysicalMergeJoin merge -> new HashJoinIterator(
                    buildIterator(merge.left(), failureCollector),
                    buildIterator(merge.right(), failureCollector),
                    merge.leftKeys(), merge.rightKeys(), merge.joinType());
            case PhysicalUnionAll union -> buildUnionIterator(union, failureCollector);
            case PhysicalSort sort -> buildIterator(sort.input(), failureCollector);
            case PhysicalLimit limit -> new LimitIterator(
                    buildIterator(limit.input(), failureCollector), limit.limit(), limit.offset());
            case PhysicalAggregate agg -> buildIterator(agg.input(), failureCollector);
        };
    }

    private RowIterator buildUnionIterator(PhysicalUnionAll union, PartialFailureCollector collector) {
        var iterators = union.inputs().stream()
                .map(p -> buildIterator(p, collector))
                .toList();
        return new UnionAllIterator(iterators);
    }

    public record QueryResult(
            List<Map<String, Object>> rows,
            List<String> columns,
            java.util.Set<String> backendsUsed,
            long executionTimeMs,
            PartialFailureReport failureReport
    ) {}

    public record StreamingQueryResult(
            RowIterator iterator,
            PartialFailureCollector failureCollector,
            FailureMode failureMode
    ) implements AutoCloseable {
        @Override
        public void close() {
            iterator.close();
        }
    }
}
