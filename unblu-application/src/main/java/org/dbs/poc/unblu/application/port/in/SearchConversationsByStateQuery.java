package org.dbs.poc.unblu.application.port.in;

import java.util.Objects;

/**
 * Query CQRS encapsulant la demande de recherche de conversations par état.
 *
 * @param state l'état recherché (INACTIVE, ACTIVE, ENDED, ONBOARDING, OFFBOARDING)
 */
public record SearchConversationsByStateQuery(String state) {

    public SearchConversationsByStateQuery {
        Objects.requireNonNull(state, "State cannot be null");
        if (state.isBlank()) {
            throw new IllegalArgumentException("State cannot be blank");
        }
    }
}
