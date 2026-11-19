package org.wrj.haifa.ai.deerflow.run;

public class RunCancelledException extends RuntimeException {

    public RunCancelledException(String runId, String reason) {
        super("Run cancelled: " + runId + (reason == null || reason.isBlank() ? "" : " (" + reason + ")"));
    }
}
