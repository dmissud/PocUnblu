package org.dbs.poc.unblu.domain.model;

import java.util.Objects;

/**
 * Informations sur une zone nommée (Named Area) Unblu.
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
