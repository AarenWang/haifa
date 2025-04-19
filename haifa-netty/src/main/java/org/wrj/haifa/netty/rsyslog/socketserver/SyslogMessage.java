package org.wrj.haifa.netty.rsyslog.socketserver;

import java.time.LocalDateTime;

public class SyslogMessage {
    private int priority;
    private int version;
    private LocalDateTime timestamp;
    private String hostname;
    private String appName;
    private String procId;
    private String msgId;
    private String structuredData;
    private String message;

    // Getters and Setters for each field
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getProcId() {
        return procId;
    }

    public void setProcId(String procId) {
        this.procId = procId;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getStructuredData() {
        return structuredData;
    }

    public void setStructuredData(String structuredData) {
        this.structuredData = structuredData;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "SyslogMessage{" +
                "priority=" + priority +
                ", version=" + version +
                ", timestamp=" + timestamp +
                ", hostname='" + hostname + '\'' +
                ", appName='" + appName + '\'' +
                ", procId='" + procId + '\'' +
                ", msgId='" + msgId + '\'' +
                ", structuredData='" + structuredData + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
