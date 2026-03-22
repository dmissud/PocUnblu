package org.dbs.poc.unblu.application.port.in;

/**
 * Requête de listing paginé des conversations historisées.
 *
 * <p>Le numéro de page est 0-indexé. La taille est bornée entre 1 et 100
 * pour éviter les requêtes abusives.
 */
public record ListConversationHistoryQuery(int page, int size) {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    public ListConversationHistoryQuery {
        if (page < 0) throw new IllegalArgumentException("Page must be >= 0, got: " + page);
        if (size < 1 || size > MAX_SIZE)
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_SIZE + ", got: " + size);
    }

    public static ListConversationHistoryQuery firstPage() {
        return new ListConversationHistoryQuery(0, DEFAULT_SIZE);
    }

    public static ListConversationHistoryQuery of(int page, int size) {
        return new ListConversationHistoryQuery(page, size > 0 ? size : DEFAULT_SIZE);
    }
}
