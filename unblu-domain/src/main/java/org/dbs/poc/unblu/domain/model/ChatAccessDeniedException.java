package org.dbs.poc.unblu.domain.model;

public class ChatAccessDeniedException extends RuntimeException {
    
    private final String reason;

    public ChatAccessDeniedException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
