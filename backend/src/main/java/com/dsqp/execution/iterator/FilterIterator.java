package com.dsqp.execution.iterator;

import com.dsqp.model.Row;
import com.dsqp.model.Schema;

import java.util.regex.Pattern;

public class FilterIterator implements RowIterator {

    private final RowIterator input;
    private final String predicate;
    private final Pattern eqPattern = Pattern.compile("([\\w.\"]+)\\s*=\\s*'?([^']+)'?");

    public FilterIterator(RowIterator input, String predicate) {
        this.input = input;
        this.predicate = predicate;
    }

    @Override
    public Schema schema() {
        return input.schema();
    }

    @Override
    public Row next() {
        Row row;
        while ((row = input.next()) != null) {
            if (matches(row)) {
                return row;
            }
        }
        return null;
    }

    private boolean matches(Row row) {
        if ("TRUE".equalsIgnoreCase(predicate.trim())) return true;

        for (String clause : predicate.split("\\s+AND\\s+")) {
            clause = clause.trim();
            var m = eqPattern.matcher(clause);
            if (m.matches()) {
                String col = m.group(1).replace("\"", "");
                String expected = m.group(2).replace("'", "");
                Object actual = row.get(col);
                if (actual == null) {
                    String shortCol = col.contains(".") ? col.substring(col.lastIndexOf('.') + 1) : col;
                    actual = row.get(shortCol);
                }
                if (actual == null || !actual.toString().equals(expected)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void close() {
        input.close();
    }
}
