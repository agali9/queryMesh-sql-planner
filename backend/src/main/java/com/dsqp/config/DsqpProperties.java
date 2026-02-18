package com.dsqp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "dsqp")
public record DsqpProperties(
        List<BackendConfig> backends,
        CatalogConfig catalog,
        ExecutionConfig execution
) {
    public DsqpProperties {
        backends = backends != null ? List.copyOf(backends) : List.of();
    }

    public record BackendConfig(
            String id,
            String name,
            String jdbcUrl,
            String username,
            String password,
            int poolSize
    ) {}

    public record CatalogConfig(Map<String, TableConfig> tables) {
        public CatalogConfig {
            tables = tables != null ? Map.copyOf(tables) : Map.of();
        }
    }

    public record TableConfig(String backendId, List<String> columns) {
        public TableConfig {
            columns = columns != null ? List.copyOf(columns) : List.of();
        }
    }

    public record ExecutionConfig(
            long queryTimeoutMs,
            int fetchSize,
            boolean parallelBackendFetch,
            String failureMode,
            int retryAttempts,
            long retryBackoffMs
    ) {
        public ExecutionConfig {
            if (queryTimeoutMs <= 0) queryTimeoutMs = 30_000;
            if (fetchSize <= 0) fetchSize = 256;
            if (retryAttempts < 0) retryAttempts = 0;
            if (retryBackoffMs < 0) retryBackoffMs = 100;
        }
    }
}
