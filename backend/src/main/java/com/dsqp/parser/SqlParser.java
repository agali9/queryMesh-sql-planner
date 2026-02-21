package com.dsqp.parser;

import com.dsqp.ir.*;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses ANSI SQL into a logical query plan using JSqlParser.
 * Focuses on SELECT with JOINs, WHERE, GROUP BY, ORDER BY, LIMIT.
 */
@Component
public class SqlParser {

    public ParsedQuery parse(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select select)) {
                throw new ParseException("Only SELECT statements are supported");
            }
            PlainSelect plainSelect = select.getPlainSelect();
            if (plainSelect == null) {
                throw new ParseException("Only simple SELECT queries are supported (no UNION/CTE yet)");
            }
            LogicalPlan plan = buildPlan(plainSelect);
            return new ParsedQuery(sql, plan);
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException("Failed to parse SQL: " + e.getMessage(), e);
        }
    }

    private LogicalPlan buildPlan(PlainSelect select) {
        LogicalPlan fromPlan = buildFrom(select.getFromItem(), select.getJoins());

        if (select.getWhere() != null) {
            fromPlan = new LogicalFilter(fromPlan, select.getWhere().toString());
        }

        if (select.getGroupBy() != null) {
            List<String> groupCols = select.getGroupBy().getGroupByExpressionList().stream()
                    .map(Object::toString)
                    .toList();
            List<AggregateExpr> aggs = new ArrayList<>();
            if (select.getSelectItems() != null) {
                for (SelectItem<?> item : select.getSelectItems()) {
                    Expression expr = item.getExpression();
                    if (expr instanceof net.sf.jsqlparser.expression.Function func) {
                        String col = func.getParameters() != null && !func.getParameters().isEmpty()
                                ? func.getParameters().get(0).toString()
                                : "*";
                        String alias = item.getAlias() != null ? item.getAlias().getName() : func.getName();
                        aggs.add(new AggregateExpr(func.getName().toUpperCase(), col, alias));
                    }
                }
            }
            fromPlan = new LogicalAggregate(fromPlan, groupCols, aggs);
        }

        if (select.getSelectItems() != null && select.getGroupBy() == null) {
            List<ProjectionExpr> projections = new ArrayList<>();
            for (SelectItem<?> item : select.getSelectItems()) {
                String expr = item.getExpression().toString();
                String alias = item.getAlias() != null
                        ? item.getAlias().getName()
                        : expr.contains(".") ? expr.substring(expr.lastIndexOf('.') + 1) : expr;
                projections.add(new ProjectionExpr(expr, alias));
            }
            fromPlan = new LogicalProject(fromPlan, projections);
        }

        if (select.getOrderByElements() != null) {
            List<SortKey> sortKeys = select.getOrderByElements().stream()
                    .map(o -> new SortKey(
                            o.getExpression().toString(),
                            !o.isAscDescPresent() || o.isAsc()
                    ))
                    .toList();
            fromPlan = new LogicalSort(fromPlan, sortKeys);
        }

        if (select.getLimit() != null) {
            int limit = select.getLimit().getRowCount() != null
                    ? Integer.parseInt(select.getLimit().getRowCount().toString())
                    : Integer.MAX_VALUE;
            int offset = select.getLimit().getOffset() != null
                    ? Integer.parseInt(select.getLimit().getOffset().toString())
                    : 0;
            fromPlan = new LogicalLimit(fromPlan, limit, offset);
        }

        return fromPlan;
    }

    private LogicalPlan buildFrom(FromItem fromItem, List<Join> joins) {
        LogicalPlan plan = buildFromItem(fromItem);

        if (joins != null) {
            for (Join join : joins) {
                LogicalPlan right = buildFromItem(join.getRightItem());
                JoinType joinType = resolveJoinType(join);
                String condition = join.getOnExpressions() != null && !join.getOnExpressions().isEmpty()
                        ? join.getOnExpressions().iterator().next().toString()
                        : "TRUE";
                plan = new LogicalJoin(plan, right, joinType, condition);
            }
        }
        return plan;
    }

    private LogicalPlan buildFromItem(FromItem item) {
        if (item instanceof Table table) {
            String name = table.getName().replace("\"", "");
            String alias = table.getAlias() != null ? table.getAlias().getName() : name;
            return new LogicalScan(name, alias);
        }
        if (item instanceof ParenthesedSelect sub) {
            throw new ParseException("Subqueries in FROM clause not yet supported");
        }
        throw new ParseException("Unsupported FROM item: " + item.getClass().getSimpleName());
    }

    private JoinType resolveJoinType(Join join) {
        if (join.isInner()) return JoinType.INNER;
        if (join.isLeft()) return JoinType.LEFT;
        if (join.isRight()) return JoinType.RIGHT;
        if (join.isFull()) return JoinType.FULL;
        return JoinType.INNER;
    }

    public record ParsedQuery(String originalSql, LogicalPlan logicalPlan) {}
}
