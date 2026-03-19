package com.dsqp.failure;

import com.dsqp.config.DsqpProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PartialFailureTest {

    @Test
    void collectorBuildsDegradedReport() {
        var collector = new PartialFailureCollector();
        collector.record(new BackendFailure(
                "backend-c", "PostgreSQL C", "Connection refused",
                FailurePhase.CONNECTION, Instant.now()
        ));

        var report = collector.buildReport(FailureMode.PARTIAL_RESULTS);
        assertTrue(report.queryDegraded());
        assertEquals(1, report.failures().size());
        assertEquals(FailureMode.PARTIAL_RESULTS, report.appliedMode());
    }

    @Test
    void failFastThrowsOnUnavailableBackend() {
        var handler = new FailureHandler(new DsqpProperties(
                List.of(), null,
                new DsqpProperties.ExecutionConfig(30_000, 256, true, "FAIL_FAST", 0, 0)
        ));
        var collector = new PartialFailureCollector();
        collector.record(new BackendFailure(
                "backend-b", "B", "timeout", FailurePhase.EXECUTION, Instant.now()
        ));

        assertThrows(PartialFailureException.class, () ->
                handler.validateOrDegrade(
                        Set.of("backend-a", "backend-b"),
                        Set.of("backend-b"),
                        collector,
                        FailureMode.FAIL_FAST
                )
        );
    }

    @Test
    void partialResultsAllowsContinuation() {
        var handler = new FailureHandler(new DsqpProperties(
                List.of(), null,
                new DsqpProperties.ExecutionConfig(30_000, 256, true, "PARTIAL_RESULTS", 0, 0)
        ));
        var collector = new PartialFailureCollector();
        collector.record(new BackendFailure(
                "backend-c", "C", "down", FailurePhase.CONNECTION, Instant.now()
        ));

        assertDoesNotThrow(() ->
                handler.validateOrDegrade(
                        Set.of("backend-a", "backend-c"),
                        Set.of("backend-c"),
                        collector,
                        FailureMode.PARTIAL_RESULTS
                )
        );
    }
}
