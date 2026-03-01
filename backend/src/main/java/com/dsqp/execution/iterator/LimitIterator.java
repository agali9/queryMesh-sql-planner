package com.dsqp.execution.iterator;

import com.dsqp.model.Row;
import com.dsqp.model.Schema;

public class LimitIterator implements RowIterator {

    private final RowIterator input;
    private final int limit;
    private final int offset;
    private int returned;
    private int skipped;

    public LimitIterator(RowIterator input, int limit, int offset) {
        this.input = input;
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    public Schema schema() {
        return input.schema();
    }

    @Override
    public Row next() {
        while (skipped < offset) {
            if (input.next() == null) return null;
            skipped++;
        }
        if (returned >= limit) return null;
        Row row = input.next();
        if (row != null) returned++;
        return row;
    }

    @Override
    public void close() {
        input.close();
    }
}
