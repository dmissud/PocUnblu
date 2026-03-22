package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.PersonInfo;

import java.util.List;

/**
 * Cas d'utilisation : recherche de personnes dans Unblu avec filtres optionnels.
 */
public interface SearchPersonsUseCase {

    /**
     * Recherche des personnes dans Unblu selon les critères de la requête.
     *
     * @param query critères de recherche (sourceId, personSource)
     * @return liste des personnes correspondantes
     */
    List<PersonInfo> searchPersons(SearchPersonsQuery query);
}
