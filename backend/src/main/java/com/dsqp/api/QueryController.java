package com.dsqp.api;

import com.dsqp.catalog.Catalog;
import com.dsqp.execution.ExecutionEngine;
import com.dsqp.execution.backend.BackendConnectionPool;
import com.dsqp.failure.BackendFailure;
import com.dsqp.failure.FailureMode;
import com.dsqp.service.QueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class QueryController {

    private final QueryService queryService;
    private final Catalog catalog;
    private final BackendConnectionPool connectionPool;

    public QueryController(QueryService queryService, Catalog catalog, BackendConnectionPool connectionPool) {
        this.queryService = queryService;
        this.catalog = catalog;
        this.connectionPool = connectionPool;
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> executeQuery(@Valid @RequestBody QueryRequest request) {
        FailureMode mode = request.failureMode() != null
                ? FailureMode.valueOf(request.failureMode())
                : null;
        var result = queryService.execute(request.sql(), mode);
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/explain")
    public ResponseEntity<QueryService.QueryPlanExplanation> explainQuery(@Valid @RequestBody QueryRequest request) {
        return ResponseEntity.ok(queryService.explain(request.sql()));
    }

    @GetMapping("/catalog/tables")
    public ResponseEntity<Map<String, Catalog.TableMetadata>> listTables() {
        var tables = catalog.allTableNames().stream()
                .map(name -> catalog.getTable(name).orElseThrow())
                .collect(java.util.stream.Collectors.toMap(
                        Catalog.TableMetadata::name,
                        t -> t
                ));
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/backends/health")
    public ResponseEntity<Map<String, BackendConnectionPool.BackendHealth>> backendHealth() {
        return ResponseEntity.ok(connectionPool.allHealth());
    }

    private QueryResponse toResponse(ExecutionEngine.QueryResult result) {
        return new QueryResponse(
                result.rows(),
                result.columns(),
                result.backendsUsed(),
                result.executionTimeMs(),
                result.failureReport().queryDegraded(),
                result.failureReport().failures().stream()
                        .map(f -> new BackendFailureDto(
                                f.backendId(), f.backendName(), f.errorMessage(),
                                f.phase().name()))
                        .toList()
        );
    }

    public record QueryRequest(
            @NotBlank String sql,
            String failureMode
    ) {}

    public record QueryResponse(
            List<Map<String, Object>> rows,
            List<String> columns,
            java.util.Set<String> backendsUsed,
            long executionTimeMs,
            boolean degraded,
            List<BackendFailureDto> failures
    ) {}

    public record BackendFailureDto(
            String backendId,
            String backendName,
            String errorMessage,
            String phase
    ) {}
}
