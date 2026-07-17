package org.wrj.haifa.ai.deerflow.model;

/** Raised when opaque provider protocol state cannot be safely preserved or restored. */
public class ModelProtocolStateException extends IllegalStateException {

    public ModelProtocolStateException(String message) {
        super(message);
    }

    public ModelProtocolStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
