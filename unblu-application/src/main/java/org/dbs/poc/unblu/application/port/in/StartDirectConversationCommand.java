package org.dbs.poc.unblu.application.port.in;

import java.util.Objects;

/**
 * Commande pour démarrer une conversation directe entre un visiteur virtuel et un agent.
 * Objet immuable — tous les champs obligatoires sont vérifiés à la construction.
 *
 * @param virtualParticipantSourceId  sourceId Unblu du visiteur virtuel (obligatoire)
 * @param agentParticipantSourceId    sourceId Unblu de l'agent (obligatoire)
 * @param subject                     sujet de la conversation (optionnel)
 */
public record StartDirectConversationCommand(
        String virtualParticipantSourceId,
        String agentParticipantSourceId,
        String subject
) {
    public StartDirectConversationCommand(String virtualParticipantSourceId, String agentParticipantSourceId, String subject) {
        this.virtualParticipantSourceId = Objects.requireNonNull(virtualParticipantSourceId, "virtualParticipantSourceId is required");
        this.agentParticipantSourceId = Objects.requireNonNull(agentParticipantSourceId, "agentParticipantSourceId is required");
        this.subject = subject;
    }
}
