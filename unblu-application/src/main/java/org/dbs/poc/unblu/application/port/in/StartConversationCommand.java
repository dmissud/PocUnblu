package org.dbs.poc.unblu.application.port.in;

import java.util.Objects;

/**
 * Commande pour démarrer une conversation.
 * Objet immuable.
 */
public record StartConversationCommand(
        String clientId,
        String subject,
        String origin
) {
    public StartConversationCommand {
        Objects.requireNonNull(clientId, "clientId is required");
        Objects.requireNonNull(origin, "origin is required");
    }
}
