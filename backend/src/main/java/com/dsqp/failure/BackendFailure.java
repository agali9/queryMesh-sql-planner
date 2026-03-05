package com.dsqp.failure;

import java.time.Instant;

public record BackendFailure(
        String backendId,
        String backendName,
        String errorMessage,
        FailurePhase phase,
        Instant timestamp
) {}
