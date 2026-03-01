package com.dsqp.execution.iterator;

import com.dsqp.model.Row;
import com.dsqp.model.Schema;

import java.util.Iterator;
import java.util.List;

/**
 * Concatenates multiple iterators — used for UNION ALL across backends.
 */
public class UnionAllIterator implements RowIterator {

    private final List<RowIterator> inputs;
    private int currentIndex;
    private Schema schema;

    public UnionAllIterator(List<RowIterator> inputs) {
        this.inputs = inputs;
    }

    @Override
    public Schema schema() {
        if (schema == null && !inputs.isEmpty()) {
            schema = inputs.getFirst().schema();
        }
        return schema != null ? schema : new Schema(List.of());
    }

    @Override
    public Row next() {
        while (currentIndex < inputs.size()) {
            Row row = inputs.get(currentIndex).next();
            if (row != null) return row;
            currentIndex++;
        }
        return null;
    }

    @Override
    public void close() {
        inputs.forEach(RowIterator::close);
    }
}
