package com.dsqp.execution.iterator;

import com.dsqp.execution.backend.BackendConnectionPool;
import com.dsqp.execution.backend.BackendException;
import com.dsqp.failure.BackendFailure;
import com.dsqp.failure.FailurePhase;
import com.dsqp.failure.PartialFailureCollector;
import com.dsqp.model.ColumnDef;
import com.dsqp.model.Row;
import com.dsqp.model.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Streams rows from a PostgreSQL backend using fetch-size controlled cursors.
 */
public class BackendScanIterator implements RowIterator {

    private static final Logger log = LoggerFactory.getLogger(BackendScanIterator.class);

    private final String backendId;
    private final String sql;
    private final List<String> columns;
    private final BackendConnectionPool pool;
    private final int fetchSize;
    private final PartialFailureCollector failureCollector;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private Schema schema;
    private boolean exhausted;
    private boolean failed;

    public BackendScanIterator(
            String backendId,
            String sql,
            List<String> columns,
            BackendConnectionPool pool,
            int fetchSize,
            PartialFailureCollector failureCollector
    ) {
        this.backendId = backendId;
        this.sql = sql;
        this.columns = columns;
        this.pool = pool;
        this.fetchSize = fetchSize;
        this.failureCollector = failureCollector;
    }

    private void init() {
        if (connection != null) return;
        try {
            connection = pool.getConnection(backendId);
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            statement.setFetchSize(fetchSize);
            resultSet = statement.executeQuery(sql);
            schema = buildSchema();
            log.debug("Backend {} executing: {}", backendId, sql);
        } catch (SQLException e) {
            failed = true;
            exhausted = true;
            pool.markUnhealthy(backendId, e.getMessage());
            failureCollector.record(new BackendFailure(
                    backendId,
                    pool.getConfig(backendId).map(c -> c.name()).orElse(backendId),
                    e.getMessage(),
                    FailurePhase.CONNECTION,
                    Instant.now()
            ));
            closeQuietly();
        }
    }

    @Override
    public Schema schema() {
        init();
        return schema != null ? schema : new Schema(List.of());
    }

    @Override
    public Row next() {
        if (exhausted || failed) return null;
        init();
        if (failed) return null;

        try {
            if (resultSet.next()) {
                return rowFromResultSet();
            }
            exhausted = true;
            return null;
        } catch (SQLException e) {
            failed = true;
            exhausted = true;
            pool.markUnhealthy(backendId, e.getMessage());
            failureCollector.record(new BackendFailure(
                    backendId,
                    pool.getConfig(backendId).map(c -> c.name()).orElse(backendId),
                    e.getMessage(),
                    FailurePhase.STREAMING,
                    Instant.now()
            ));
            return null;
        }
    }

    private Row rowFromResultSet() throws SQLException {
        var meta = resultSet.getMetaData();
        int colCount = meta.getColumnCount();
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 1; i <= colCount; i++) {
            String label = meta.getColumnLabel(i);
            values.put(label, resultSet.getObject(i));
        }
        return new Row(values);
    }

    private Schema buildSchema() throws SQLException {
        var meta = resultSet.getMetaData();
        int colCount = meta.getColumnCount();
        var cols = new java.util.ArrayList<ColumnDef>();
        for (int i = 1; i <= colCount; i++) {
            cols.add(new ColumnDef(meta.getColumnLabel(i), meta.getColumnTypeName(i)));
        }
        return new Schema(cols);
    }

    @Override
    public void close() {
        closeQuietly();
    }

    private void closeQuietly() {
        try { if (resultSet != null) resultSet.close(); } catch (SQLException ignored) {}
        try { if (statement != null) statement.close(); } catch (SQLException ignored) {}
        try {
            if (connection != null) {
                connection.commit();
                connection.close();
            }
        } catch (SQLException ignored) {}
    }

    public boolean isFailed() {
        return failed;
    }

    public String backendId() {
        return backendId;
    }
}
