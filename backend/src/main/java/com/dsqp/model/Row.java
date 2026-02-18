package com.dsqp.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable row representation flowing through the streaming execution pipeline.
 */
public record Row(Map<String, Object> values) {

    public Row {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Object get(String column) {
        return values.get(column);
    }

    public Object get(String tableAlias, String column) {
        return values.get(tableAlias + "." + column);
    }

    public Row with(String column, Object value) {
        Map<String, Object> merged = new LinkedHashMap<>(values);
        merged.put(column, value);
        return new Row(merged);
    }

    public Row merge(Row other) {
        Map<String, Object> merged = new LinkedHashMap<>(values);
        merged.putAll(other.values);
        return new Row(merged);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Row row)) return false;
        return Objects.equals(values, row.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
