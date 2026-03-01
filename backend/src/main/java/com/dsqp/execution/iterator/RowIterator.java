package com.dsqp.execution.iterator;

import com.dsqp.model.Row;
import com.dsqp.model.Schema;

/**
 * Volcano-style iterator interface for pipelined query execution.
 * Operators pull rows incrementally without materializing full intermediate results.
 */
public interface RowIterator extends AutoCloseable {

    Schema schema();

    /**
     * Advances to the next row. Returns null when exhausted.
     */
    Row next();

    @Override
    default void close() {}
}
