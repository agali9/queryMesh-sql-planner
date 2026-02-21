package com.dsqp.planner;

import com.dsqp.catalog.Catalog;
import com.dsqp.ir.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Transforms logical plans into physical plans with backend-specific subqueries.
 * Uses partition-aware pushdown: single-backend subtrees execute entirely on that backend.
 */
@Component
public class QueryPlanner {

    private static final Pattern JOIN_EQ_PATTERN = Pattern.compile(
            "([\\w.\"]+)\\s*=\\s*([\\w.\"]+)"
    );

    private final Catalog catalog;

    public QueryPlanner(Catalog catalog) {
        this.catalog = catalog;
    }

    public PlannedQuery plan(LogicalPlan logical) {
        PhysicalPlan physical = transform(logical);
        Set<String> backends = physical.involvedBackends();
        return new PlannedQuery(logical, physical, backends);
    }

    private PhysicalPlan transform(LogicalPlan node) {
        return switch (node) {
            case LogicalScan scan -> planScan(scan);
            case LogicalFilter filter -> new PhysicalFilter(transform(filter.input()), filter.predicate());
            case LogicalProject project -> new PhysicalProject(transform(project.input()), project.projections());
            case LogicalJoin join -> planJoin(join);
            case LogicalAggregate agg -> new PhysicalAggregate(
                    transform(agg.input()), agg.groupByColumns(), agg.aggregates());
            case LogicalSort sort -> new PhysicalSort(transform(sort.input()), sort.sortKeys());
            case LogicalLimit limit -> new PhysicalLimit(transform(limit.input()), limit.limit(), limit.offset());
        };
    }

    private PhysicalPlan planScan(LogicalScan scan) {
        var tableMeta = catalog.getTable(scan.tableName())
                .orElseThrow(() -> new PlannerException("Unknown table: " + scan.tableName()));

        String sql = "SELECT * FROM " + scan.tableName();
        if (!scan.alias().equals(scan.tableName())) {
            sql += " AS " + scan.alias();
        }

        return new PhysicalBackendScan(
                tableMeta.backendId(),
                sql,
                tableMeta.columns()
        );
    }

    private PhysicalPlan planJoin(LogicalJoin join) {
        PhysicalPlan left = transform(join.left());
        PhysicalPlan right = transform(join.right());

        Set<String> leftBackends = left.involvedBackends();
        Set<String> rightBackends = right.involvedBackends();

        // Co-located join: both sides on same backend → push join down
        if (leftBackends.size() == 1 && leftBackends.equals(rightBackends)) {
            return pushdownJoin(left, right, join);
        }

        // Cross-backend join: use hash join with streaming iterators
        JoinKeyPair keys = extractJoinKeys(join.condition());
        return new PhysicalHashJoin(left, right, keys.leftKeys(), keys.rightKeys(), join.joinType());
    }

    private PhysicalPlan pushdownJoin(PhysicalPlan left, PhysicalPlan right, LogicalJoin join) {
        if (left instanceof PhysicalBackendScan leftScan && right instanceof PhysicalBackendScan rightScan) {
            String sql = String.format(
                    "SELECT * FROM (%s) AS l JOIN (%s) AS r ON %s",
                    leftScan.rewrittenSql(),
                    rightScan.rewrittenSql(),
                    join.condition()
            );
            List<String> cols = new ArrayList<>(leftScan.outputColumns());
            cols.addAll(rightScan.outputColumns());
            return new PhysicalBackendScan(leftScan.backendId(), sql, cols);
        }

        JoinKeyPair keys = extractJoinKeys(join.condition());
        return new PhysicalHashJoin(left, right, keys.leftKeys(), keys.rightKeys(), join.joinType());
    }

    private JoinKeyPair extractJoinKeys(String condition) {
        List<String> leftKeys = new ArrayList<>();
        List<String> rightKeys = new ArrayList<>();

        for (String part : condition.split("\\s+AND\\s+")) {
            Matcher m = JOIN_EQ_PATTERN.matcher(part.trim());
            if (m.matches()) {
                leftKeys.add(cleanKey(m.group(1)));
                rightKeys.add(cleanKey(m.group(2)));
            }
        }

        if (leftKeys.isEmpty()) {
            leftKeys.add(cleanKey(condition.split("=")[0].trim()));
            rightKeys.add(cleanKey(condition.split("=")[1].trim()));
        }
        return new JoinKeyPair(leftKeys, rightKeys);
    }

    private static String cleanKey(String key) {
        return key.replace("\"", "").trim();
    }

    public record PlannedQuery(LogicalPlan logical, PhysicalPlan physical, Set<String> involvedBackends) {}
    private record JoinKeyPair(List<String> leftKeys, List<String> rightKeys) {}
}
