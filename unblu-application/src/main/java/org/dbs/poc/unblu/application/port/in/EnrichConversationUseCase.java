package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.history.ConversationHistory;

/**
 * Cas d'utilisation : enrichissement à la demande d'une conversation depuis Unblu.
 *
 * <p>Récupère les participants et les messages de la conversation dans Unblu
 * et les persiste en base de données.
 */
public interface EnrichConversationUseCase {

    /**
     * Enrichit la conversation identifiée par {@code conversationId} avec les données
     * remontées depuis Unblu (participants + messages) et retourne l'entité mise à jour.
     *
     * @param conversationId identifiant Unblu de la conversation
     * @return l'entité {@link ConversationHistory} après enrichissement
     * @throws IllegalArgumentException si la conversation n'est pas trouvée en base
     */
    ConversationHistory enrichOne(String conversationId);
}
