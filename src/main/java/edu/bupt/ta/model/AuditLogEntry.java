package edu.bupt.ta.model;

import java.time.LocalDateTime;

public class AuditLogEntry {
    private LocalDateTime timestamp;
    private String actorUserId;
    private String action;
    private String detail;

    public AuditLogEntry() {
    }

    public AuditLogEntry(LocalDateTime timestamp, String actorUserId, String action, String detail) {
        this.timestamp = timestamp;
        this.actorUserId = actorUserId;
        this.action = action;
        this.detail = detail;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String toLine() {
        return timestamp + " | " + actorUserId + " | " + action + " | " + detail;
    }
}
