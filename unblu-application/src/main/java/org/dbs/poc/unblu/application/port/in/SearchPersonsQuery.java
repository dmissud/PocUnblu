package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.PersonSource;

/**
 * Requête pour rechercher des personnes.
 */
public record SearchPersonsQuery(
        String sourceId,
        PersonSource personSource
) {
}
