package com.dsqp.catalog;

import com.dsqp.config.DsqpProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Metadata catalog mapping logical tables to physical PostgreSQL backends.
 */
@Component
public class Catalog {

    private final Map<String, TableMetadata> tables;
    private final Map<String, BackendMetadata> backends;

    public Catalog(DsqpProperties properties) {
        this.backends = properties.backends().stream()
                .collect(Collectors.toMap(
                        DsqpProperties.BackendConfig::id,
                        b -> new BackendMetadata(b.id(), b.name(), b.jdbcUrl())
                ));
        this.tables = properties.catalog().tables().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            DsqpProperties.TableConfig tc = e.getValue();
                            BackendMetadata backend = backends.get(tc.backendId());
                            if (backend == null) {
                                throw new IllegalStateException(
                                        "Table '" + e.getKey() + "' references unknown backend: " + tc.backendId());
                            }
                            return new TableMetadata(e.getKey(), tc.backendId(), tc.columns());
                        }
                ));
    }

    public Optional<TableMetadata> getTable(String name) {
        return Optional.ofNullable(tables.get(normalize(name)));
    }

    public BackendMetadata getBackend(String backendId) {
        BackendMetadata backend = backends.get(backendId);
        if (backend == null) {
            throw new CatalogException("Unknown backend: " + backendId);
        }
        return backend;
    }

    public Set<String> resolveBackendsForTables(Set<String> tableNames) {
        return tableNames.stream()
                .map(Catalog::normalize)
                .map(tables::get)
                .filter(t -> t != null)
                .map(TableMetadata::backendId)
                .collect(Collectors.toSet());
    }

    public List<TableMetadata> tablesOnBackend(String backendId) {
        return tables.values().stream()
                .filter(t -> t.backendId().equals(backendId))
                .toList();
    }

    public Set<String> allTableNames() {
        return Set.copyOf(tables.keySet());
    }

    public Map<String, BackendMetadata> allBackends() {
        return Map.copyOf(backends);
    }

    private static String normalize(String name) {
        return name.toLowerCase().replace("\"", "");
    }

    public record TableMetadata(String name, String backendId, List<String> columns) {}

    public record BackendMetadata(String id, String name, String jdbcUrl) {}
}
