package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.SearchPersonsQuery;
import org.dbs.poc.unblu.application.port.in.SearchPersonsUseCase;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonQueryService implements SearchPersonsUseCase {

    private final UnbluPort unbluPort;

    @Override
    public List<PersonInfo> searchPersons(SearchPersonsQuery query) {
        log.info("Recherche de personnes connues dans Unblu, sourceId: {}", query.getSourceId());
        return unbluPort.searchPersons(query.getSourceId());
    }
}
