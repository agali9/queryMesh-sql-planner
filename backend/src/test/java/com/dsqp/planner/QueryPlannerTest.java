package com.dsqp.planner;

import com.dsqp.catalog.Catalog;
import com.dsqp.config.DsqpProperties;
import com.dsqp.ir.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QueryPlannerTest {

    private QueryPlanner planner;

    @BeforeEach
    void setUp() {
        var props = new DsqpProperties(
                List.of(
                        new DsqpProperties.BackendConfig("backend-a", "A", "jdbc:postgresql://localhost:5432/a", "u", "p", 5),
                        new DsqpProperties.BackendConfig("backend-b", "B", "jdbc:postgresql://localhost:5433/b", "u", "p", 5),
                        new DsqpProperties.BackendConfig("backend-c", "C", "jdbc:postgresql://localhost:5434/c", "u", "p", 5)
                ),
                new DsqpProperties.CatalogConfig(Map.of(
                        "users", new DsqpProperties.TableConfig("backend-a", List.of("id", "name")),
                        "orders", new DsqpProperties.TableConfig("backend-b", List.of("id", "user_id")),
                        "inventory", new DsqpProperties.TableConfig("backend-c", List.of("id", "product_id"))
                )),
                new DsqpProperties.ExecutionConfig(30_000, 256, true, "PARTIAL_RESULTS", 2, 100)
        );
        planner = new QueryPlanner(new Catalog(props));
    }

    @Test
    void singleTableScanRoutesToCorrectBackend() {
        LogicalPlan plan = new LogicalScan("users", "users");
        var result = planner.plan(plan);

        assertInstanceOf(PhysicalBackendScan.class, result.physical());
        var scan = (PhysicalBackendScan) result.physical();
        assertEquals("backend-a", scan.backendId());
        assertEquals(Set.of("backend-a"), result.involvedBackends());
    }

    @Test
    void coLocatedJoinPushesDownToSingleBackend() {
        LogicalPlan plan = new LogicalJoin(
                new LogicalScan("users", "u"),
                new LogicalScan("users", "u2"),
                JoinType.INNER,
                "u.id = u2.id"
        );
        var result = planner.plan(plan);
        assertEquals(Set.of("backend-a"), result.involvedBackends());
    }

    @Test
    void crossBackendJoinUsesHashJoin() {
        LogicalPlan plan = new LogicalJoin(
                new LogicalScan("users", "u"),
                new LogicalScan("orders", "o"),
                JoinType.INNER,
                "u.id = o.user_id"
        );
        var result = planner.plan(plan);

        assertInstanceOf(PhysicalHashJoin.class, result.physical());
        assertEquals(Set.of("backend-a", "backend-b"), result.involvedBackends());
    }

    @Test
    void threeWayCrossBackendJoin() {
        LogicalPlan plan = new LogicalJoin(
                new LogicalJoin(
                        new LogicalScan("users", "u"),
                        new LogicalScan("orders", "o"),
                        JoinType.INNER,
                        "u.id = o.user_id"
                ),
                new LogicalScan("inventory", "i"),
                JoinType.INNER,
                "o.id = i.product_id"
        );
        var result = planner.plan(plan);

        assertInstanceOf(PhysicalHashJoin.class, result.physical());
        assertEquals(Set.of("backend-a", "backend-b", "backend-c"), result.involvedBackends());
    }
}
