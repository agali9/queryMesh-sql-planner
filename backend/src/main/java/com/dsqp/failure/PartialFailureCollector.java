package com.dsqp.failure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-safe collector for backend failures during query execution.
 */
public class PartialFailureCollector {

    private final List<BackendFailure> failures = Collections.synchronizedList(new ArrayList<>());

    public void record(BackendFailure failure) {
        failures.add(failure);
    }

    public PartialFailureReport buildReport(FailureMode mode) {
        List<BackendFailure> snapshot = List.copyOf(failures);
        boolean degraded = !snapshot.isEmpty() && mode != FailureMode.FAIL_FAST;
        return new PartialFailureReport(snapshot, mode, degraded);
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }
}
