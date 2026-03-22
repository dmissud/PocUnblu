package org.dbs.poc.unblu.domain.model;

/**
 * Exception levée lorsqu'un client est refusé à l'accès au chat par le moteur de règles.
 */
public class ChatAccessDeniedException extends RuntimeException {

    private final String reason;

    /**
     * Construit une exception d'accès refusé au chat.
     *
     * @param message message d'erreur lisible
     * @param reason  raison métier du refus (issue du moteur de règles)
     */
    public ChatAccessDeniedException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    /**
     * Retourne la raison métier du refus d'accès.
     *
     * @return la raison du refus
     */
    public String getReason() {
        return reason;
    }
}
