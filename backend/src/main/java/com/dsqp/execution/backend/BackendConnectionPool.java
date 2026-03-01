package com.dsqp.execution.backend;

import com.dsqp.config.DsqpProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BackendConnectionPool {

    private final Map<String, DataSource> pools;
    private final Map<String, DsqpProperties.BackendConfig> configs;
    private final Map<String, BackendHealth> healthStatus = new ConcurrentHashMap<>();

    public BackendConnectionPool(Map<String, DataSource> pools, List<DsqpProperties.BackendConfig> configs) {
        this.pools = Map.copyOf(pools);
        this.configs = configs.stream()
                .collect(java.util.stream.Collectors.toMap(DsqpProperties.BackendConfig::id, c -> c));
        configs.forEach(c -> healthStatus.put(c.id(), BackendHealth.healthy(c.id(), c.name())));
    }

    public Connection getConnection(String backendId) throws SQLException {
        DataSource ds = pools.get(backendId);
        if (ds == null) {
            throw new BackendException("Unknown backend: " + backendId);
        }
        try {
            Connection conn = ds.getConnection();
            markHealthy(backendId);
            return conn;
        } catch (SQLException e) {
            markUnhealthy(backendId, e.getMessage());
            throw e;
        }
    }

    public Optional<DsqpProperties.BackendConfig> getConfig(String backendId) {
        return Optional.ofNullable(configs.get(backendId));
    }

    public Map<String, BackendHealth> allHealth() {
        return Map.copyOf(healthStatus);
    }

    public void markHealthy(String backendId) {
        configs.computeIfPresent(backendId, (id, cfg) -> {
            healthStatus.put(id, BackendHealth.healthy(id, cfg.name()));
            return cfg;
        });
    }

    public void markUnhealthy(String backendId, String reason) {
        configs.computeIfPresent(backendId, (id, cfg) -> {
            healthStatus.put(id, BackendHealth.unhealthy(id, cfg.name(), reason));
            return cfg;
        });
    }

    public record BackendHealth(String backendId, String name, boolean healthy, String lastError) {
        static BackendHealth healthy(String id, String name) {
            return new BackendHealth(id, name, true, null);
        }

        static BackendHealth unhealthy(String id, String name, String error) {
            return new BackendHealth(id, name, false, error);
        }
    }
}
