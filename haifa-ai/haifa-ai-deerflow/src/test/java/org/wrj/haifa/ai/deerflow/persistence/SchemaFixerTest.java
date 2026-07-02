package org.wrj.haifa.ai.deerflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

class SchemaFixerTest {

    @Test
    void widensDeerflowRunsStatusConstraintToAllowSuspended() throws Exception {
        Path db = Files.createTempFile("deerflow-schema-fixer-", ".sqlite");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource("jdbc:sqlite:" + db));

        jdbcTemplate.execute("""
                CREATE TABLE deerflow_runs (
                  run_id VARCHAR(64) PRIMARY KEY,
                  thread_id VARCHAR(64) NOT NULL,
                  mode VARCHAR(32),
                  model_name VARCHAR(128),
                  status VARCHAR(32) CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED','CANCELLED','TIMEOUT')),
                  error VARCHAR(4000),
                  metadata_json VARCHAR(4000),
                  research_options_json VARCHAR(4000),
                  created_at TIMESTAMP NOT NULL,
                  updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX idx_runs_thread_id_created_at ON deerflow_runs(thread_id, created_at)");
        jdbcTemplate.update("""
                INSERT INTO deerflow_runs
                (run_id, thread_id, mode, model_name, status, error, metadata_json, research_options_json, created_at, updated_at)
                VALUES ('run-1', 'thread-1', 'chat', 'model', 'RUNNING', NULL, '{}', '{}', '2026-07-02T00:00:00Z', '2026-07-02T00:00:00Z')
                """);

        SchemaFixer fixer = new SchemaFixer();
        ReflectionTestUtils.setField(fixer, "jdbcTemplate", jdbcTemplate);
        fixer.fixCheckConstraints();

        jdbcTemplate.update("UPDATE deerflow_runs SET status='SUSPENDED' WHERE run_id='run-1'");

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM deerflow_runs WHERE run_id='run-1'", String.class);
        String createSql = jdbcTemplate.queryForObject(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='deerflow_runs'", String.class);
        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sqlite_master WHERE type='index' AND name='idx_runs_thread_id_created_at'",
                Integer.class);

        assertThat(status).isEqualTo("SUSPENDED");
        assertThat(createSql).contains("SUSPENDED");
        assertThat(indexCount).isEqualTo(1);
    }
}
