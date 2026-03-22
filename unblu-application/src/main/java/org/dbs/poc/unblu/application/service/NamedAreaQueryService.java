package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.SearchAgentsByNamedAreaUseCase;
import org.dbs.poc.unblu.application.port.in.SearchNamedAreasUseCase;
import org.dbs.poc.unblu.domain.model.NamedAreaInfo;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Implémentation des cas d'utilisation {@link SearchNamedAreasUseCase} et {@link SearchAgentsByNamedAreaUseCase}.
 * Délègue les recherches au port secondaire {@link UnbluPort}.
 */
public class NamedAreaQueryService implements SearchNamedAreasUseCase, SearchAgentsByNamedAreaUseCase {

    private final UnbluPort unbluPort;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NamedAreaInfo> searchNamedAreas() {
        log.info("Récupération des zones nommées Unblu");
        return unbluPort.searchNamedAreas();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PersonInfo> searchAgentsByNamedArea(String namedAreaId) {
        log.info("Récupération des agents pour la named area: {}", namedAreaId);
        return unbluPort.searchAgentsByNamedArea(namedAreaId);
    }
}
