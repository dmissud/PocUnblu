package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.history.ConversationHistory;

import java.util.Optional;

/**
 * Cas d'utilisation : consultation du détail d'une conversation avec l'intégralité
 * de ses événements dans l'ordre chronologique.
 */
public interface GetConversationHistoryUseCase {

    /**
     * Retourne le détail complet d'une conversation identifiée par son identifiant Unblu.
     *
     * @param conversationId l'identifiant Unblu de la conversation
     * @return l'historique complet (événements + participants), ou {@link Optional#empty()} si introuvable
     */
    Optional<ConversationHistory> getByConversationId(String conversationId);
}
