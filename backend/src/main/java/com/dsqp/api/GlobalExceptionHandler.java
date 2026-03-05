package com.dsqp.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.dsqp.parser.ParseException;
import com.dsqp.planner.PlannerException;
import com.dsqp.failure.PartialFailureException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<Map<String, String>> handleParse(ParseException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "type", "PARSE_ERROR"));
    }

    @ExceptionHandler(PlannerException.class)
    public ResponseEntity<Map<String, String>> handlePlanner(PlannerException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "type", "PLANNER_ERROR"));
    }

    @ExceptionHandler(PartialFailureException.class)
    public ResponseEntity<Map<String, Object>> handlePartialFailure(PartialFailureException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", e.getMessage(),
                "type", "PARTIAL_FAILURE",
                "report", e.report()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception e) {
        return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage(), "type", "INTERNAL_ERROR"));
    }
}
