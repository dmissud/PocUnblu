package org.dbs.poc.unblu.domain.model.history;

import java.util.List;
import java.util.Objects;

/**
 * Résultat paginé d'une requête sur l'historique des conversations.
 *
 * <p>Value object immuable encapsulant les éléments de la page courante
 * ainsi que les métadonnées de pagination nécessaires à la navigation côté client.
 */
public record ConversationHistoryPage(
        List<ConversationHistory> items,
        long totalItems,
        int page,
        int size,
        int totalPages) {

    public ConversationHistoryPage {
        Objects.requireNonNull(items, "items cannot be null");
        items = List.copyOf(items);
    }
}
