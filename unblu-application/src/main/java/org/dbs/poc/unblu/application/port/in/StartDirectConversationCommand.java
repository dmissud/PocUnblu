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
    public StartDirectConversationCommand {
        Objects.requireNonNull(virtualParticipantSourceId, "virtualParticipantSourceId is required");
        Objects.requireNonNull(agentParticipantSourceId, "agentParticipantSourceId is required");
    }
}
