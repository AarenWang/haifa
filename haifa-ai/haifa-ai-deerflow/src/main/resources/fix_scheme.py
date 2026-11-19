import sqlite3
import re

DB_PATH = r"D:\workspace\haifa\haifa-ai\haifa-ai-deerflow\data\deerflow.sqlite"

TABLE = "deerflow_agent_loop_runs"
OLD_TABLE = TABLE + "_old"


def add_suspended_to_status_check(create_sql: str) -> str:
    """
    把 status 的 CHECK 约束：
        status in ('PENDING','RUNNING','COMPLETED','FAILED','CANCELLED','TIMEOUT')
    改成：
        status in ('PENDING','RUNNING','SUSPENDED','COMPLETED','FAILED','CANCELLED','TIMEOUT')
    """

    pattern = re.compile(
        r"(status\s+[^,]*?\s+check\s*\(\s*status\s+in\s*\()([^)]+)(\)\s*\))",
        re.IGNORECASE
    )

    match = pattern.search(create_sql)

    if not match:
        raise RuntimeError(
            "没有匹配到 status CHECK 约束，请检查 CREATE TABLE 语句。"
        )

    prefix = match.group(1)
    values_text = match.group(2)
    suffix = match.group(3)

    values = re.findall(r"'([^']+)'", values_text)

    if "SUSPENDED" in values:
        print("表结构里已经包含 SUSPENDED，不需要迁移。")
        return create_sql

    new_values = []

    for value in values:
        new_values.append(value)

        # 放在 RUNNING 后面，比较符合状态流转语义
        if value == "RUNNING":
            new_values.append("SUSPENDED")

    # 如果原来没有 RUNNING，就兜底追加到最后
    if "SUSPENDED" not in new_values:
        new_values.append("SUSPENDED")

    new_values_text = ",".join(f"'{value}'" for value in new_values)

    return create_sql[:match.start()] + prefix + new_values_text + suffix + create_sql[match.end():]


conn = sqlite3.connect(DB_PATH)

try:
    conn.execute("PRAGMA foreign_keys = OFF")
    conn.execute("BEGIN")

    row = conn.execute(
        """
        SELECT sql
        FROM sqlite_schema
        WHERE type = 'table'
          AND name = ?
        """,
        (TABLE,)
    ).fetchone()

    if not row:
        raise RuntimeError(f"Table not found: {TABLE}")

    create_sql = row[0]

    print("原始建表语句：")
    print(create_sql)

    new_create_sql = add_suspended_to_status_check(create_sql)

    if new_create_sql == create_sql:
        conn.rollback()
        raise SystemExit(0)

    print("\n新的建表语句：")
    print(new_create_sql)

    indexes = conn.execute(
        """
        SELECT name, sql
        FROM sqlite_schema
        WHERE type = 'index'
          AND tbl_name = ?
          AND sql IS NOT NULL
        """,
        (TABLE,)
    ).fetchall()

    triggers = conn.execute(
        """
        SELECT name, sql
        FROM sqlite_schema
        WHERE type = 'trigger'
          AND tbl_name = ?
          AND sql IS NOT NULL
        """,
        (TABLE,)
    ).fetchall()

    columns = conn.execute(f'PRAGMA table_info("{TABLE}")').fetchall()
    column_names = [col[1] for col in columns]
    column_list = ", ".join(f'"{name}"' for name in column_names)

    conn.execute(f'ALTER TABLE "{TABLE}" RENAME TO "{OLD_TABLE}"')

    conn.execute(new_create_sql)

    conn.execute(
        f"""
        INSERT INTO "{TABLE}" ({column_list})
        SELECT {column_list}
        FROM "{OLD_TABLE}"
        """
    )

    conn.execute(f'DROP TABLE "{OLD_TABLE}"')

    for index_name, index_sql in indexes:
        print(f"重建索引: {index_name}")
        conn.execute(index_sql)

    for trigger_name, trigger_sql in triggers:
        print(f"重建触发器: {trigger_name}")
        conn.execute(trigger_sql)

    conn.commit()
    print("\n迁移完成。")

except Exception:
    conn.rollback()
    raise

finally:
    conn.execute("PRAGMA foreign_keys = ON")
    conn.close()