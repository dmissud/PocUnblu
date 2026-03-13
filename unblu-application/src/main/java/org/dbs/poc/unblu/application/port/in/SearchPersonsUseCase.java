package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.PersonInfo;

import java.util.List;

public interface SearchPersonsUseCase {
    List<PersonInfo> searchPersons(SearchPersonsQuery query);
}
