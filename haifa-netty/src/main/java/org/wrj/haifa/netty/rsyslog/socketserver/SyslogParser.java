package org.wrj.haifa.netty.rsyslog.socketserver;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SyslogParser {

    public SyslogMessage parse(String syslogData) {
        SyslogMessage syslogMessage = new SyslogMessage();
        try {
            // PRI: Extract the priority (e.g., "<34>")
            int priStart = syslogData.indexOf('<') + 1;
            int priEnd = syslogData.indexOf('>');
            String priStr = syslogData.substring(priStart, priEnd);
            syslogMessage.setPriority(Integer.parseInt(priStr));

            // HEADER: Split the remaining part of the message
            String[] parts = syslogData.substring(priEnd + 1).split(" ", 7);
            syslogMessage.setVersion(Integer.parseInt(parts[0]));

            // Timestamp
            syslogMessage.setTimestamp(LocalDateTime.parse(parts[1], DateTimeFormatter.ISO_DATE_TIME));

            // Hostname
            syslogMessage.setHostname(parts[2]);

            // AppName
            syslogMessage.setAppName(parts[3]);

            // ProcID
            syslogMessage.setProcId(parts[4]);

            // MsgID
            syslogMessage.setMsgId(parts[5]);

            // Structured Data and Message
            String structuredDataAndMsg = parts[6];
            if (structuredDataAndMsg.startsWith("[")) {
                int msgStartIndex = structuredDataAndMsg.indexOf("] ") + 2;
                syslogMessage.setStructuredData(structuredDataAndMsg.substring(0, msgStartIndex));
                syslogMessage.setMessage(structuredDataAndMsg.substring(msgStartIndex));
            } else {
                syslogMessage.setStructuredData("-");
                syslogMessage.setMessage(structuredDataAndMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return syslogMessage;
    }
}
