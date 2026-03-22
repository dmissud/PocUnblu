package org.dbs.poc.unblu.application.port.in;

import java.util.Objects;

/**
 * Commande pour démarrer une conversation avec une équipe.
 * Objet immuable — tous les champs obligatoires sont vérifiés à la construction.
 *
 * @param clientId  identifiant du client (obligatoire)
 * @param subject   sujet de la conversation (optionnel)
 * @param origin    origine de la demande, ex. URL ou canal (obligatoire)
 * @param teamId    identifiant Unblu de l'équipe destinataire (obligatoire)
 */
public record StartConversationCommand(
        String clientId,
        String subject,
        String origin,
        String teamId
) {
    public StartConversationCommand(String clientId, String subject, String origin, String teamId) {
        this.clientId = Objects.requireNonNull(clientId, "clientId is required");
        this.subject = subject;
        this.origin = Objects.requireNonNull(origin, "origin is required");
        this.teamId = Objects.requireNonNull(teamId, "teamId is required");
    }
}
