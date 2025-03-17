package org.wrj.haifa.netty.rsyslog.client;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * RFC5424 message format implementation
 * <pre>
 * The Syslog message format defined in RFC 5424:
 * <PRIVAL>VERSION TIMESTAMP HOSTNAME APP-NAME PROCID MSGID STRUCTURED-DATA MSG
 * </pre>
 */
public class RFC5424Message {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    .withZone(ZoneId.systemDefault());
    
    // Facility values
    public static final int FACILITY_KERNEL = 0;
    public static final int FACILITY_USER = 1;
    public static final int FACILITY_MAIL = 2;
    public static final int FACILITY_SYSTEM_DAEMONS = 3;
    public static final int FACILITY_SECURITY = 4;
    public static final int FACILITY_SYSLOGD = 5;
    public static final int FACILITY_LINE_PRINTER = 6;
    public static final int FACILITY_NETWORK_NEWS = 7;
    public static final int FACILITY_UUCP = 8;
    public static final int FACILITY_CLOCK_DAEMON = 9;
    public static final int FACILITY_SECURITY2 = 10;
    public static final int FACILITY_FTP_DAEMON = 11;
    public static final int FACILITY_NTP = 12;
    public static final int FACILITY_LOG_AUDIT = 13;
    public static final int FACILITY_LOG_ALERT = 14;
    public static final int FACILITY_CLOCK_DAEMON2 = 15;
    public static final int FACILITY_LOCAL0 = 16;
    public static final int FACILITY_LOCAL1 = 17;
    public static final int FACILITY_LOCAL2 = 18;
    public static final int FACILITY_LOCAL3 = 19;
    public static final int FACILITY_LOCAL4 = 20;
    public static final int FACILITY_LOCAL5 = 21;
    public static final int FACILITY_LOCAL6 = 22;
    public static final int FACILITY_LOCAL7 = 23;
    
    // Severity values
    public static final int SEVERITY_EMERGENCY = 0;
    public static final int SEVERITY_ALERT = 1;
    public static final int SEVERITY_CRITICAL = 2;
    public static final int SEVERITY_ERROR = 3;
    public static final int SEVERITY_WARNING = 4;
    public static final int SEVERITY_NOTICE = 5;
    public static final int SEVERITY_INFORMATIONAL = 6;
    public static final int SEVERITY_DEBUG = 7;
    
    private int facility;
    private int severity;
    private String timestamp;
    private String hostname;
    private String appName;
    private String procId;
    private String msgId;
    private String structuredData;
    private String message;
    
    public RFC5424Message() {
        this.facility = FACILITY_USER;
        this.severity = SEVERITY_NOTICE;
        this.timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
        this.hostname = getLocalHostname();
        this.appName = "-";
        this.procId = "-";
        this.msgId = "-";
        this.structuredData = "-";
        this.message = "";
    }
    
    private String getLocalHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }
    
    /**
     * Formats the message according to RFC 5424
     * @return byte array containing the formatted message
     */
    public byte[] format() {
        // Calculate priority value: facility * 8 + severity
        int prival = facility * 8 + severity;
        
        StringBuilder sb = new StringBuilder();
        sb.append('<').append(prival).append('>');
        sb.append('1').append(' '); // RFC 5424 version
        sb.append(timestamp).append(' ');
        sb.append(hostname).append(' ');
        sb.append(appName).append(' ');
        sb.append(procId).append(' ');
        sb.append(msgId).append(' ');
        sb.append(structuredData);
        
        if (message != null && !message.isEmpty()) {
            sb.append(' ').append(message);
        }
        
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    // Getters and setters
    public int getFacility() {
        return facility;
    }
    
    public RFC5424Message setFacility(int facility) {
        this.facility = facility;
        return this;
    }
    
    public int getSeverity() {
        return severity;
    }
    
    public RFC5424Message setSeverity(int severity) {
        this.severity = severity;
        return this;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public RFC5424Message setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public RFC5424Message setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public RFC5424Message setAppName(String appName) {
        this.appName = appName;
        return this;
    }
    
    public String getProcId() {
        return procId;
    }
    
    public RFC5424Message setProcId(String procId) {
        this.procId = procId;
        return this;
    }
    
    public String getMsgId() {
        return msgId;
    }
    
    public RFC5424Message setMsgId(String msgId) {
        this.msgId = msgId;
        return this;
    }
    
    public String getStructuredData() {
        return structuredData;
    }
    
    public RFC5424Message setStructuredData(String structuredData) {
        this.structuredData = structuredData;
        return this;
    }
    
    public String getMessage() {
        return message;
    }
    
    public RFC5424Message setMessage(String message) {
        this.message = message;
        return this;
    }
} 