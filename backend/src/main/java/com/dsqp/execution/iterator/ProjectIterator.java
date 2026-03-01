package com.dsqp.execution.iterator;

import com.dsqp.model.ColumnDef;
import com.dsqp.model.Row;
import com.dsqp.model.Schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjectIterator implements RowIterator {

    private final RowIterator input;
    private final List<String> expressions;
    private final List<String> aliases;
    private Schema schema;

    public ProjectIterator(RowIterator input, List<String> expressions, List<String> aliases) {
        this.input = input;
        this.expressions = expressions;
        this.aliases = aliases;
    }

    @Override
    public Schema schema() {
        if (schema == null) {
            var cols = new ArrayList<ColumnDef>();
            for (String alias : aliases) {
                cols.add(new ColumnDef(alias, "unknown"));
            }
            schema = new Schema(cols);
        }
        return schema;
    }

    @Override
    public Row next() {
        Row row = input.next();
        if (row == null) return null;

        Map<String, Object> projected = new LinkedHashMap<>();
        for (int i = 0; i < expressions.size(); i++) {
            String expr = expressions.get(i);
            String alias = aliases.get(i);
            Object value = resolveExpression(row, expr);
            projected.put(alias, value);
        }
        return new Row(projected);
    }

    private Object resolveExpression(Row row, String expr) {
        if ("*".equals(expr.trim())) {
            return row.values().values().stream().findFirst().orElse(null);
        }
        Object val = row.get(expr);
        if (val != null) return val;
        String shortName = expr.contains(".") ? expr.substring(expr.lastIndexOf('.') + 1) : expr;
        return row.get(shortName);
    }

    @Override
    public void close() {
        input.close();
    }
}
