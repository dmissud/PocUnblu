package org.dbs.poc.unblu.application.port.in;

import java.util.Objects;

/**
 * Commande pour démarrer une conversation directe.
 * Objet immuable.
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
