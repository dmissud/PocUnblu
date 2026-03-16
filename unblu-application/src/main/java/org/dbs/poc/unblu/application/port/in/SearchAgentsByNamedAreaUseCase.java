package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.PersonInfo;

import java.util.List;

public interface SearchAgentsByNamedAreaUseCase {
    List<PersonInfo> searchAgentsByNamedArea(String namedAreaId);
}
