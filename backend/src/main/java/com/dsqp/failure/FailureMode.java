package com.dsqp.failure;

/**
 * Defines behavior when one or more backends are unavailable during query execution.
 */
public enum FailureMode {
    /** Return results from available backends; annotate response with missing partitions. */
    PARTIAL_RESULTS,
    /** Skip unavailable backends entirely; continue with remaining shards. */
    SKIP_BACKEND,
    /** Fail the entire query if any required backend is unavailable. */
    FAIL_FAST
}
