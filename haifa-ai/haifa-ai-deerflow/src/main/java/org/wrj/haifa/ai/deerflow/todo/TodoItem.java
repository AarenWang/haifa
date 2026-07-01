package org.wrj.haifa.ai.deerflow.todo;

/**
 * Represents a single task entry in the Agent's todo checklist.
 */
public class TodoItem {

    private String id;
    private String content;
    private String status; // "pending", "in_progress", "completed"

    public TodoItem() {
    }

    public TodoItem(String id, String content, String status) {
        this.id = id;
        this.content = content;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
