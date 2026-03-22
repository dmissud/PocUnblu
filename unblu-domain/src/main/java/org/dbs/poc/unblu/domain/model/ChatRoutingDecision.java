package org.dbs.poc.unblu.domain.model;

import java.util.Objects;

/**
 * Décision de routage prise par le moteur de règles.
 *
 * @param isAuthorized          {@code true} si le client est autorisé à démarrer un chat
 * @param unbluAssignedGroupId  identifiant du groupe Unblu cible (obligatoire si autorisé)
 * @param routingReason         motif textuel de la décision (ex. segment, règle appliquée)
 */
public record ChatRoutingDecision(
        boolean isAuthorized,
        String unbluAssignedGroupId,
        String routingReason
) {
    public ChatRoutingDecision {
        if (isAuthorized) {
            Objects.requireNonNull(unbluAssignedGroupId, "unbluAssignedGroupId must be provided for authorized chats");
        }
    }
}
