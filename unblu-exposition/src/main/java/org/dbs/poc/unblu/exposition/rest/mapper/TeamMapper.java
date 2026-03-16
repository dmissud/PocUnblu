package org.dbs.poc.unblu.exposition.rest.mapper;

import org.apache.camel.Exchange;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.exposition.rest.dto.TeamResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper for team-related DTO transformations.
 * Handles conversion between domain models and REST DTOs.
 */
@Component
public class TeamMapper {

    /**
     * Maps a TeamInfo domain model to a TeamResponse DTO.
     */
    public TeamResponse toResponse(TeamInfo teamInfo) {
        return TeamResponse.builder()
                .id(teamInfo.id())
                .name(teamInfo.name())
                .description(teamInfo.description())
                .build();
    }

    /**
     * Maps a list of TeamInfo to a list of TeamResponse.
     */
    public List<TeamResponse> toResponseList(List<TeamInfo> teamInfos) {
        if (teamInfos == null) {
            return Collections.emptyList();
        }

        return teamInfos.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Maps list of TeamInfo from Exchange body to list of TeamResponse.
     */
    public void mapTeamsToResponse(Exchange exchange) {
        List<?> rawList = exchange.getIn().getBody(List.class);
        if (rawList == null) {
            exchange.getIn().setBody(Collections.emptyList());
            return;
        }

        List<TeamInfo> teamInfos = rawList.stream()
                .filter(TeamInfo.class::isInstance)
                .map(TeamInfo.class::cast)
                .toList();

        exchange.getIn().setBody(toResponseList(teamInfos));
    }
}
