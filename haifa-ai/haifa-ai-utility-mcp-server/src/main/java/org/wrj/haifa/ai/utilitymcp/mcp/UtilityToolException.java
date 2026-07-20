package org.wrj.haifa.ai.utilitymcp.mcp;

public class UtilityToolException extends RuntimeException {

    private final UtilityErrorCode code;
    private final boolean retryable;

    public UtilityToolException(UtilityErrorCode code, String message, boolean retryable) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    public UtilityToolException(UtilityErrorCode code, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.retryable = retryable;
    }

    public UtilityErrorCode code() { return code; }
    public boolean retryable() { return retryable; }

    public static UtilityToolException invalid(String message) {
        return new UtilityToolException(UtilityErrorCode.INVALID_ARGUMENT, message, false);
    }
}
