package com.dsqp.model;

import java.util.List;

public record Schema(List<ColumnDef> columns) {

    public List<String> columnNames() {
        return columns.stream().map(ColumnDef::name).toList();
    }
}
