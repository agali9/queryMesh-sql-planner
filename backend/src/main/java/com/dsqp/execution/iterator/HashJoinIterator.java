package com.dsqp.execution.iterator;

import com.dsqp.ir.JoinType;
import com.dsqp.model.Row;
import com.dsqp.model.Schema;

import java.util.*;

/**
 * Hash join operator — builds an in-memory hash table on the build side,
 * then probes with rows from the probe side. Supports streaming on probe side.
 */
public class HashJoinIterator implements RowIterator {

    private final RowIterator buildSide;
    private final RowIterator probeSide;
    private final List<String> buildKeys;
    private final List<String> probeKeys;
    private final JoinType joinType;

    private Map<List<Object>, List<Row>> hashTable;
    private Row currentProbeRow;
    private Iterator<Row> currentMatches;
    private boolean buildComplete;
    private Schema schema;

    public HashJoinIterator(
            RowIterator buildSide,
            RowIterator probeSide,
            List<String> buildKeys,
            List<String> probeKeys,
            JoinType joinType
    ) {
        this.buildSide = buildSide;
        this.probeSide = probeSide;
        this.buildKeys = buildKeys;
        this.probeKeys = probeKeys;
        this.joinType = joinType;
    }

    private void buildHashTable() {
        if (buildComplete) return;
        hashTable = new HashMap<>();
        Row row;
        while ((row = buildSide.next()) != null) {
            List<Object> key = extractKey(row, buildKeys);
            hashTable.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        buildComplete = true;
    }

    @Override
    public Schema schema() {
        if (schema == null) {
            var buildCols = buildSide.schema().columns();
            var probeCols = probeSide.schema().columns();
            var merged = new ArrayList<>(buildCols);
            merged.addAll(probeCols);
            schema = new Schema(merged);
        }
        return schema;
    }

    @Override
    public Row next() {
        buildHashTable();

        while (true) {
            if (currentMatches != null && currentMatches.hasNext()) {
                return currentMatches.next().merge(currentProbeRow);
            }

            currentProbeRow = probeSide.next();
            if (currentProbeRow == null) {
                return null;
            }

            List<Object> key = extractKey(currentProbeRow, probeKeys);
            List<Row> matches = hashTable.getOrDefault(key, List.of());

            if (matches.isEmpty()) {
                if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
                    return currentProbeRow;
                }
                continue;
            }

            currentMatches = matches.stream()
                    .map(buildRow -> buildRow.merge(currentProbeRow))
                    .iterator();

            if (currentMatches.hasNext()) {
                return currentMatches.next();
            }
        }
    }

    private List<Object> extractKey(Row row, List<String> keys) {
        return keys.stream().map(k -> {
            Object val = row.get(k);
            if (val == null) {
                String shortKey = k.contains(".") ? k.substring(k.lastIndexOf('.') + 1) : k;
                val = row.get(shortKey);
            }
            return val;
        }).toList();
    }

    @Override
    public void close() {
        buildSide.close();
        probeSide.close();
    }
}
