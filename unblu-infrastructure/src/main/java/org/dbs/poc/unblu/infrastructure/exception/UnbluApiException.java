package org.dbs.poc.unblu.infrastructure.exception;

import lombok.Getter;

/**
 * Exception technique de la couche infrastructure représentant une erreur retournée
 * par l'API Unblu via le SDK Jersey.
 * À distinguer de {@link org.dbs.poc.unblu.domain.exception.UnbluApiException} qui est
 * l'exception de domaine correspondante.
 */
@Getter
public class UnbluApiException extends RuntimeException {
    /**
     * Code de statut HTTP retourné par l'API Unblu.
     */
    private final int statusCode;
    /** Description courte du statut HTTP (ex. {@code "Not Found"}, {@code "Forbidden"}). */
    private final String statusDescription;

    /**
     * @param statusCode        code HTTP retourné par Unblu
     * @param statusDescription description courte du statut
     * @param message           message d'erreur détaillé
     */
    public UnbluApiException(int statusCode, String statusDescription, String message) {
        super(message);
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
    }
}
