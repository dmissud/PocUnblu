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
    public StartConversationCommand(String clientId, String subject, String origin) {
        this.clientId = Objects.requireNonNull(clientId, "clientId is required");
        this.subject = subject;
        this.origin = Objects.requireNonNull(origin, "origin is required");
    }
}
