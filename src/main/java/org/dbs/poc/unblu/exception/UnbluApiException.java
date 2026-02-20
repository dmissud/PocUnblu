package org.dbs.poc.unblu.exception;

import lombok.Getter;

@Getter
public class UnbluApiException extends RuntimeException {
    private final int statusCode;
    private final String statusDescription;

    public UnbluApiException(int statusCode, String statusDescription, String message) {
        super(message);
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
    }
}
