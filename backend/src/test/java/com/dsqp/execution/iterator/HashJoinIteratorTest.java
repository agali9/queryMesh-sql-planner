package com.dsqp.execution.iterator;

import com.dsqp.ir.JoinType;
import com.dsqp.model.ColumnDef;
import com.dsqp.model.Row;
import com.dsqp.model.Schema;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HashJoinIteratorTest {

    @Test
    void innerJoinProducesMatchingRows() {
        var buildSide = new ListIterator(List.of(
                row("id", 1, "name", "Alice"),
                row("id", 2, "name", "Bob")
        ));
        var probeSide = new ListIterator(List.of(
                row("user_id", 1, "total", 100),
                row("user_id", 3, "total", 200)
        ));

        var join = new HashJoinIterator(
                buildSide, probeSide,
                List.of("id"), List.of("user_id"),
                JoinType.INNER
        );

        Row result = join.next();
        assertNotNull(result);
        assertEquals("Alice", result.get("name"));
        assertEquals(100, result.get("total"));

        assertNull(join.next());
    }

    @Test
    void leftJoinRetainsUnmatchedProbeRows() {
        var buildSide = new ListIterator(List.of(row("id", 1, "name", "Alice")));
        var probeSide = new ListIterator(List.of(row("user_id", 99, "total", 500)));

        var join = new HashJoinIterator(
                buildSide, probeSide,
                List.of("id"), List.of("user_id"),
                JoinType.LEFT
        );

        Row result = join.next();
        assertNotNull(result);
        assertEquals(500, result.get("total"));
    }

    private static Row row(Object... kvPairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            map.put((String) kvPairs[i], kvPairs[i + 1]);
        }
        return new Row(map);
    }

    private static class ListIterator implements RowIterator {
        private final List<Row> rows;
        private int index;

        ListIterator(List<Row> rows) {
            this.rows = rows;
        }

        @Override
        public Schema schema() {
            return new Schema(List.of(new ColumnDef("col", "text")));
        }

        @Override
        public Row next() {
            return index < rows.size() ? rows.get(index++) : null;
        }
    }
}
