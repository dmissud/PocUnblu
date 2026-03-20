package org.dbs.poc.unblu.infrastructure.exception;

import org.dbs.poc.unblu.domain.exception.UnbluApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnbluApiException.class)
    public ResponseEntity<Map<String, Object>> handleUnbluApiException(UnbluApiException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getStatusDescription());
        response.put("message", ex.getMessage());
        response.put("statusCode", ex.getStatusCode());

        HttpStatus status = switch (ex.getStatusCode()) {
            case 403 -> HttpStatus.FORBIDDEN;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status).body(response);
    }
}
