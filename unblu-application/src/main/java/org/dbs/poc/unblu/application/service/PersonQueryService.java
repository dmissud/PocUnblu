package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.port.in.SearchPersonsUseCase;
import org.dbs.poc.unblu.domain.port.in.query.SearchPersonsQuery;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Implémentation du cas d'utilisation {@link SearchPersonsUseCase}.
 * Délègue la recherche au port secondaire {@link UnbluPort}.
 */
public class PersonQueryService implements SearchPersonsUseCase {

    private final UnbluPort unbluPort;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PersonInfo> searchPersons(SearchPersonsQuery query) {
        log.info("Recherche de personnes dans Unblu, sourceId: {}, personSource: {}", query.sourceId(), query.personSource());
        return unbluPort.searchPersons(query.sourceId(), query.personSource());
    }
}
