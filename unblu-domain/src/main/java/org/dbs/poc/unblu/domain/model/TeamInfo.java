package org.dbs.poc.unblu.domain.model;

import java.util.Objects;

/**
 * Informations sur une équipe (Team) Unblu.
 */
public record TeamInfo(
        String id,
        String name,
        String description
) {
    public TeamInfo {
        Objects.requireNonNull(id, "Team id cannot be null");
        Objects.requireNonNull(name, "Team name cannot be null");
    }
}
