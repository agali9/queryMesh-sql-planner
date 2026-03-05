package com.dsqp.failure;

public class PartialFailureException extends RuntimeException {

    private final PartialFailureReport report;

    public PartialFailureException(String message, PartialFailureReport report) {
        super(message);
        this.report = report;
    }

    public PartialFailureReport report() {
        return report;
    }
}
