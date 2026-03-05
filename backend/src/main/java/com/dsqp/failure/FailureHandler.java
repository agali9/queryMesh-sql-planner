package com.dsqp.failure;

import com.dsqp.config.DsqpProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Applies failure mode semantics when backends are unavailable.
 */
@Component
public class FailureHandler {

    private final FailureMode defaultMode;

    public FailureHandler(DsqpProperties properties) {
        this.defaultMode = FailureMode.valueOf(properties.execution().failureMode());
    }

    public FailureMode getDefaultMode() {
        return defaultMode;
    }

    public void validateOrDegrade(
            Set<String> requiredBackends,
            Set<String> failedBackends,
            PartialFailureCollector collector,
            FailureMode mode
    ) {
        if (failedBackends.isEmpty()) return;

        switch (mode) {
            case FAIL_FAST -> throw new PartialFailureException(
                    "Query aborted: backends unavailable: " + failedBackends,
                    collector.buildReport(mode)
            );
            case SKIP_BACKEND, PARTIAL_RESULTS -> {
                // Continue execution; failures recorded in collector
            }
        }
    }
}
