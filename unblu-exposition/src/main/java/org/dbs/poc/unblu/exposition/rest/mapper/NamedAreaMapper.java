package org.dbs.poc.unblu.exposition.rest.mapper;

import org.apache.camel.Exchange;
import org.dbs.poc.unblu.application.port.in.SearchAgentsByNamedAreaUseCase;
import org.dbs.poc.unblu.application.port.in.SearchNamedAreasUseCase;
import org.dbs.poc.unblu.domain.model.NamedAreaInfo;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.exposition.rest.dto.NamedAreaResponse;
import org.dbs.poc.unblu.exposition.rest.dto.PersonResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper for named area-related DTO transformations.
 * Handles conversion between domain models and REST DTOs.
 */
@Component
public class NamedAreaMapper {

    private final SearchNamedAreasUseCase searchNamedAreasUseCase;
    private final SearchAgentsByNamedAreaUseCase searchAgentsByNamedAreaUseCase;

    public NamedAreaMapper(SearchNamedAreasUseCase searchNamedAreasUseCase,
                          SearchAgentsByNamedAreaUseCase searchAgentsByNamedAreaUseCase) {
        this.searchNamedAreasUseCase = searchNamedAreasUseCase;
        this.searchAgentsByNamedAreaUseCase = searchAgentsByNamedAreaUseCase;
    }

    /**
     * Maps a NamedAreaInfo domain model to a NamedAreaResponse DTO.
     */
    public NamedAreaResponse toResponse(NamedAreaInfo namedAreaInfo) {
        return NamedAreaResponse.builder()
                .id(namedAreaInfo.id())
                .name(namedAreaInfo.name())
                .description(namedAreaInfo.description())
                .build();
    }

    /**
     * Maps a list of NamedAreaInfo to a list of NamedAreaResponse.
     */
    public List<NamedAreaResponse> toResponseList(List<NamedAreaInfo> namedAreaInfos) {
        if (namedAreaInfos == null) {
            return Collections.emptyList();
        }

        return namedAreaInfos.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Searches named areas and directly returns NamedAreaResponse list.
     * This method combines query execution and response mapping.
     */
    public void searchAndMapNamedAreas(Exchange exchange) {
        List<NamedAreaInfo> namedAreaInfos = searchNamedAreasUseCase.searchNamedAreas();
        exchange.getIn().setBody(toResponseList(namedAreaInfos));
    }

    /**
     * Searches agents by named area and returns PersonResponse list.
     */
    public void searchAndMapAgentsByNamedArea(Exchange exchange) {
        String namedAreaId = exchange.getIn().getHeader("namedAreaId", String.class);
        List<PersonInfo> agents = searchAgentsByNamedAreaUseCase.searchAgentsByNamedArea(namedAreaId);
        List<PersonResponse> responses = agents.stream()
                .map(agent -> PersonResponse.builder()
                        .id(agent.id())
                        .sourceId(agent.sourceId())
                        .displayName(agent.displayName())
                        .email(agent.email())
                        .build())
                .toList();
        exchange.getIn().setBody(responses);
    }
}
