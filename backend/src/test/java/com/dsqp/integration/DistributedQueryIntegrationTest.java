package com.dsqp.integration;

import com.dsqp.catalog.Catalog;
import com.dsqp.config.DsqpProperties;
import com.dsqp.execution.ExecutionEngine;
import com.dsqp.execution.backend.BackendConnectionPool;
import com.dsqp.failure.FailureHandler;
import com.dsqp.failure.FailureMode;
import com.dsqp.parser.SqlParser;
import com.dsqp.planner.QueryPlanner;
import com.dsqp.service.QueryService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration")
class DistributedQueryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresA = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shard_a")
            .withUsername("dsqp")
            .withPassword("dsqp");

    @Container
    static PostgreSQLContainer<?> postgresB = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shard_b")
            .withUsername("dsqp")
            .withPassword("dsqp");

    private static QueryService queryService;

    @BeforeAll
    static void setUp() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker not available — skipping integration tests");

        postgresA.start();
        postgresB.start();

        seedDatabase(postgresA, """
                CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100), region_id INT);
                INSERT INTO users VALUES (1, 'Alice', 1), (2, 'Bob', 2), (3, 'Carol', 1);
                """);

        seedDatabase(postgresB, """
                CREATE TABLE orders (id INT PRIMARY KEY, user_id INT, total DECIMAL(10,2));
                INSERT INTO orders VALUES (1, 1, 150.00), (2, 1, 75.50), (3, 2, 200.00), (4, 3, 50.00);
                """);

        var props = buildProperties();
        var catalog = new Catalog(props);
        var pool = new BackendConnectionPool(
                Map.of(
                        "backend-a", createDataSource(postgresA),
                        "backend-b", createDataSource(postgresB)
                ),
                props.backends()
        );
        var parser = new SqlParser();
        var planner = new QueryPlanner(catalog);
        var failureHandler = new FailureHandler(props);
        var engine = new ExecutionEngine(pool, failureHandler, props, new SimpleMeterRegistry());
        queryService = new QueryService(parser, planner, engine);
    }

    @Test
    @Order(1)
    void singleBackendQuery() {
        var result = queryService.execute("SELECT id, name FROM users WHERE region_id = 1");
        assertFalse(result.rows().isEmpty());
        assertEquals(2, result.rows().size());
        assertTrue(result.executionTimeMs() < 5000);
    }

    @Test
    @Order(2)
    void crossBackendJoin() {
        var result = queryService.execute(
                "SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id"
        );
        assertFalse(result.rows().isEmpty());
        assertEquals(2, result.backendsUsed().size());
        assertTrue(result.executionTimeMs() < 5000, "Cross-backend join should complete quickly");
    }

    @Test
    @Order(3)
    void explainPlan() {
        var explanation = queryService.explain(
                "SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id"
        );
        assertNotNull(explanation.physicalPlan());
        assertEquals(2, explanation.involvedBackends().size());
    }

    @Test
    @Order(4)
    void partialFailureModeReturnsResults() {
        var result = queryService.execute(
                "SELECT id, name FROM users",
                FailureMode.PARTIAL_RESULTS
        );
        assertFalse(result.rows().isEmpty());
        assertFalse(result.failureReport().queryDegraded());
    }

    private static void seedDatabase(PostgreSQLContainer<?> container, String sql) {
        try (var conn = container.createConnection("");
             var stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed database", e);
        }
    }

    private static javax.sql.DataSource createDataSource(PostgreSQLContainer<?> container) {
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(container.getJdbcUrl());
        config.setUsername(container.getUsername());
        config.setPassword(container.getPassword());
        config.setMaximumPoolSize(3);
        return new com.zaxxer.hikari.HikariDataSource(config);
    }

    private static DsqpProperties buildProperties() {
        return new DsqpProperties(
                List.of(
                        new DsqpProperties.BackendConfig("backend-a", "A",
                                postgresA.getJdbcUrl(), "dsqp", "dsqp", 5),
                        new DsqpProperties.BackendConfig("backend-b", "B",
                                postgresB.getJdbcUrl(), "dsqp", "dsqp", 5)
                ),
                new DsqpProperties.CatalogConfig(Map.of(
                        "users", new DsqpProperties.TableConfig("backend-a",
                                List.of("id", "name", "region_id")),
                        "orders", new DsqpProperties.TableConfig("backend-b",
                                List.of("id", "user_id", "total"))
                )),
                new DsqpProperties.ExecutionConfig(30_000, 256, true, "PARTIAL_RESULTS", 2, 100)
        );
    }
}
