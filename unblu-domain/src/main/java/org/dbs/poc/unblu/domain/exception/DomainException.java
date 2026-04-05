package org.dbs.poc.unblu.domain.exception;

/**
 * Exception de base pour toutes les erreurs métier du domaine.
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}