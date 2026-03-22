package org.dbs.poc.unblu.domain.model;

import java.util.Objects;

/**
 * Informations sur une équipe (Team) Unblu.
 *
 * @param id          identifiant Unblu de l'équipe
 * @param name        nom affiché de l'équipe
 * @param description description optionnelle
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
