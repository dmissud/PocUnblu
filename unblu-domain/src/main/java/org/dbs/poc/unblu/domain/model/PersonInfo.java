package org.dbs.poc.unblu.domain.model;

import java.util.Objects;

/**
 * Informations sur une personne (Agent ou Client).
 * Objet immuable.
 */
public record PersonInfo(
        String id,
        String sourceId,
        String displayName,
        String email,
        String firstName,
        String lastName,
        String personType
) {
    public PersonInfo {
        Objects.requireNonNull(id, "Person id cannot be null");
        Objects.requireNonNull(sourceId, "Person sourceId cannot be null");
        Objects.requireNonNull(displayName, "Person displayName cannot be null");
    }

    /**
     * Indique si cette personne est un agent (type {@code AGENT}).
     *
     * @return {@code true} si la personne est un agent
     */
    public boolean isAgent() {
        return "AGENT".equals(personType);
    }
}
