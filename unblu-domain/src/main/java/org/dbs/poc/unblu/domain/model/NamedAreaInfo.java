package org.dbs.poc.unblu.domain.model;

import java.util.Objects;

/**
 * Informations sur une zone nommée (Named Area) Unblu.
 *
 * @param id          identifiant Unblu de la zone nommée
 * @param name        nom affiché de la zone
 * @param description description optionnelle
 */
public record NamedAreaInfo(
        String id,
        String name,
        String description
) {
    public NamedAreaInfo {
        Objects.requireNonNull(id, "Named Area id cannot be null");
        Objects.requireNonNull(name, "Named Area name cannot be null");
    }
}
