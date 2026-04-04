package org.dbs.poc.unblu.domain.port.in.command;

import java.util.Objects;

/**
 * Commande pour démarrer une conversation directe entre un visiteur virtuel et un agent.
 * Objet immuable — tous les champs obligatoires sont vérifiés à la construction.
 *
 * @param virtualParticipantId       id Unblu interne du visiteur virtuel (obligatoire)
 * @param virtualParticipantSourceId sourceId Unblu du visiteur virtuel (obligatoire)
 * @param agentParticipantId         id Unblu interne de l'agent (obligatoire)
 * @param agentParticipantSourceId   sourceId Unblu de l'agent (obligatoire)
 * @param subject                    sujet de la conversation (optionnel)
 */
public record StartDirectConversationCommand(
        String virtualParticipantId,
        String virtualParticipantSourceId,
        String agentParticipantId,
        String agentParticipantSourceId,
        String subject
) {
    public StartDirectConversationCommand(String virtualParticipantId, String virtualParticipantSourceId,
                                          String agentParticipantId, String agentParticipantSourceId,
                                          String subject) {
        this.virtualParticipantId = Objects.requireNonNull(virtualParticipantId, "virtualParticipantId is required");
        this.virtualParticipantSourceId = Objects.requireNonNull(virtualParticipantSourceId, "virtualParticipantSourceId is required");
        this.agentParticipantId = Objects.requireNonNull(agentParticipantId, "agentParticipantId is required");
        this.agentParticipantSourceId = Objects.requireNonNull(agentParticipantSourceId, "agentParticipantSourceId is required");
        this.subject = subject;
    }
}
