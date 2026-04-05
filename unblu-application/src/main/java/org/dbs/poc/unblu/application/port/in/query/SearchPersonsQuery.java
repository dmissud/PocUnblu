package org.dbs.poc.unblu.application.port.in.query;

import org.dbs.poc.unblu.domain.model.PersonSource;

/**
 * Requête pour rechercher des personnes Unblu.
 *
 * @param sourceId     identifiant source (peut être {@code null} pour chercher toutes les personnes)
 * @param personSource type de source ({@link PersonSource#VIRTUAL} ou {@link PersonSource#USER_DB})
 */
public record SearchPersonsQuery(
        String sourceId,
        PersonSource personSource
) {
}
