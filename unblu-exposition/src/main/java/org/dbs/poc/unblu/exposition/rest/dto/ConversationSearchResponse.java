package org.dbs.poc.unblu.exposition.rest.dto;

import java.util.List;

/**
 * Réponse à la recherche de conversations par état.
 *
 * @param conversations la liste des conversations trouvées
 * @param totalCount    nombre total de résultats
 * @param searchedState l'état recherché
 */
public record ConversationSearchResponse(
        List<ConversationSearchItemResponse> conversations,
        int totalCount,
        String searchedState) {

    /**
     * Résumé d'une conversation retournée dans la recherche.
     *
     * @param conversationId identifiant Unblu de la conversation
     * @param topic          sujet de la conversation
     * @param state          état courant de la conversation
     * @param createdAt      horodatage de création (ISO-8601)
     * @param endedAt        horodatage de fin (ISO-8601), {@code null} si non terminée
     */
    public record ConversationSearchItemResponse(
            String conversationId,
            String topic,
            String state,
            String createdAt,
            String endedAt) {
    }
}
