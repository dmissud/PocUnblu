package org.dbs.poc.unblu.domain.model;

import java.util.Objects;

/**
 * Décision de routage prise par le moteur de règles.
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
