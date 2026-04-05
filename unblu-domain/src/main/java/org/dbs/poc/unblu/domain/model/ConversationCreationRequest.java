package org.dbs.poc.unblu.domain.model;

import lombok.Builder;

import java.util.List;

/**
 * Données de création d'une conversation dans Unblu.
 * Représenté par un record immuable avec un builder pour respecter les principes de Clean Code.
 *
 * @param topic                le sujet de la conversation
 * @param visitorData          données optionnelles du visiteur (ex: clientId)
 * @param namedAreaId          identifiant de la zone nommée (optionnel)
 * @param conversationTemplate identifiant du template de conversation (optionnel)
 * @param participants         liste des participants à ajouter à la création (optionnel)
 */
@Builder
public record ConversationCreationRequest(
        String topic,
        String visitorData,
        String namedAreaId,
        String conversationTemplate,
        List<ParticipantRequest> participants
) {
    /**
     * Représente un participant à ajouter lors de la création d'une conversation.
     *
     * @param personId          identifiant Unblu de la personne
     * @param participationType type de participation (ex: ASSIGNED_AGENT, CONTEXT_PERSON)
     */
    @Builder
    public record ParticipantRequest(
            String personId,
            String participationType
    ) {
    }
}
