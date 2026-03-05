package com.dsqp.failure;

import java.util.List;

public record PartialFailureReport(
        List<BackendFailure> failures,
        FailureMode appliedMode,
        boolean queryDegraded
) {
    public static PartialFailureReport none() {
        return new PartialFailureReport(List.of(), FailureMode.FAIL_FAST, false);
    }
}
