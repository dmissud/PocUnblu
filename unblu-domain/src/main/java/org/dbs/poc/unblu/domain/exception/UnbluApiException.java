package org.dbs.poc.unblu.domain.exception;

import lombok.Getter;

/**
 * Exception de domaine représentant une erreur retournée par l'API Unblu.
 * Transporte le code HTTP et la description du statut pour faciliter la gestion des erreurs en amont.
 */
@Getter
public class UnbluApiException extends RuntimeException {

    /**
     * Code de statut HTTP retourné par l'API Unblu.
     */
    private final int statusCode;

    /** Description courte du statut HTTP (ex. "Not Found", "Forbidden"). */
    private final String statusDescription;

    /**
     * Construit une exception d'API Unblu.
     *
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
