package org.wrj.haifa.ai.deerflow.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto-fixes SQLite CHECK constraints on startup when enum values have been
 * expanded (e.g. SUSPENDED, TIMEOUT) but the existing database schema was
 * created by an older Hibernate version with a narrower constraint.
 *
 * <p>Hibernate {@code ddl-auto: update} adds new columns/tables but never
 * drops or widens existing CHECK constraints.  This component detects the
 * mismatch and rebuilds the affected tables in-place, preserving all data.
 */
@Component
public class SchemaFixer {

    private static final Pattern STATUS_CHECK_PATTERN = Pattern.compile(
            "(status\\s+[^,]*?\\s+check\\s*\\(\\s*status\\s+in\\s*\\()([^)]+)(\\)\\s*\\))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS_VALUE_PATTERN = Pattern.compile("'([^']+)'");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixCheckConstraints() {
        fixTableIfMissingStatuses("deerflow_runs", List.of("SUSPENDED"));
        fixTableIfMissingStatuses("deerflow_agent_loop_runs", List.of("SUSPENDED", "TIMEOUT"));
    }

    private void fixTableIfMissingStatuses(String tableName, List<String> requiredStatuses) {
        try {
            String createSql = tableCreateSql(tableName);
            if (createSql == null || createSql.isBlank()) {
                return;
            }
            String patchedSql = addMissingStatuses(createSql, requiredStatuses);
            if (patchedSql.equals(createSql)) {
                return;
            }
            rebuildTable(tableName, patchedSql);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fix SQLite status CHECK constraint for table " + tableName, e);
        }
    }

    private String tableCreateSql(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM sqlite_master WHERE type='table' AND name=?",
                    Integer.class, tableName);
            if (count == null || count == 0) {
                return null;
            }
            return jdbcTemplate.queryForObject(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name=?",
                    String.class, tableName);
        } catch (Exception ex) {
            // The fixer is intentionally SQLite-specific. Other databases do not
            // expose sqlite_master and should continue startup untouched.
            return null;
        }
    }

    private String addMissingStatuses(String createSql, List<String> requiredStatuses) {
        Matcher checkMatcher = STATUS_CHECK_PATTERN.matcher(createSql);
        if (!checkMatcher.find()) {
            return createSql;
        }

        List<String> values = new ArrayList<>();
        Matcher valueMatcher = STATUS_VALUE_PATTERN.matcher(checkMatcher.group(2));
        while (valueMatcher.find()) {
            values.add(valueMatcher.group(1));
        }

        boolean changed = false;
        for (String requiredStatus : requiredStatuses) {
            if (!values.contains(requiredStatus)) {
                int insertAt = values.indexOf("RUNNING");
                if (insertAt >= 0) {
                    values.add(insertAt + 1, requiredStatus);
                } else {
                    values.add(requiredStatus);
                }
                changed = true;
            }
        }
        if (!changed) {
            return createSql;
        }

        String newValues = values.stream()
                .map(value -> "'" + value + "'")
                .reduce((left, right) -> left + "," + right)
                .orElse(checkMatcher.group(2));
        return createSql.substring(0, checkMatcher.start())
                + checkMatcher.group(1)
                + newValues
                + checkMatcher.group(3)
                + createSql.substring(checkMatcher.end());
    }

    private void rebuildTable(String tableName, String newCreateSql) {
        String oldTable = tableName + "_old";
        List<String> columnNames = columnNames(tableName);
        String columnList = quoteList(columnNames);
        List<String> indexSqls = schemaSqls("index", tableName);
        List<String> triggerSqls = schemaSqls("trigger", tableName);

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            boolean oldAutoCommit = connection.getAutoCommit();
            try (Statement statement = connection.createStatement()) {
                connection.setAutoCommit(false);
                statement.execute("PRAGMA foreign_keys = OFF");
                statement.execute("DROP TABLE IF EXISTS " + quote(oldTable));
                statement.execute("ALTER TABLE " + quote(tableName) + " RENAME TO " + quote(oldTable));
                statement.execute(newCreateSql);
                statement.execute("INSERT INTO " + quote(tableName) + " (" + columnList + ") "
                        + "SELECT " + columnList + " FROM " + quote(oldTable));
                statement.execute("DROP TABLE " + quote(oldTable));
                for (String indexSql : indexSqls) {
                    statement.execute(indexSql);
                }
                for (String triggerSql : triggerSqls) {
                    statement.execute(triggerSql);
                }
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys = ON");
                }
                connection.setAutoCommit(oldAutoCommit);
            }
            return null;
        });
    }

    private List<String> columnNames(String tableName) {
        return jdbcTemplate.queryForList("PRAGMA table_info(" + quote(tableName) + ")").stream()
                .map(row -> String.valueOf(row.get("name")))
                .toList();
    }

    private List<String> schemaSqls(String type, String tableName) {
        return jdbcTemplate.queryForList(
                        "SELECT sql FROM sqlite_master WHERE type=? AND tbl_name=? AND sql IS NOT NULL",
                        type, tableName)
                .stream()
                .map(row -> String.valueOf(row.get("sql")))
                .toList();
    }

    private String quoteList(List<String> names) {
        return names.stream()
                .map(this::quote)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow(() -> new IllegalStateException("No columns found while rebuilding SQLite table"));
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
