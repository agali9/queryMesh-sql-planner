package com.dsqp.parser;

import com.dsqp.ir.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlParserTest {

    private SqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new SqlParser();
    }

    @Test
    void parsesSimpleSelect() {
        var result = parser.parse("SELECT id, name FROM users");
        assertInstanceOf(LogicalProject.class, result.logicalPlan());
    }

    @Test
    void parsesJoinQuery() {
        var result = parser.parse(
                "SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id"
        );
        assertInstanceOf(LogicalProject.class, result.logicalPlan());
        var project = (LogicalProject) result.logicalPlan();
        assertInstanceOf(LogicalJoin.class, project.input());
    }

    @Test
    void parsesWhereClause() {
        var result = parser.parse("SELECT * FROM users WHERE region_id = 1");
        LogicalPlan plan = result.logicalPlan();
        assertTrue(plan instanceof LogicalProject || plan instanceof LogicalFilter);
    }

    @Test
    void rejectsNonSelect() {
        assertThrows(ParseException.class, () -> parser.parse("INSERT INTO users VALUES (1)"));
    }

    @Test
    void parsesLimit() {
        var result = parser.parse("SELECT * FROM users LIMIT 10");
        assertInstanceOf(LogicalLimit.class, result.logicalPlan());
    }
}
