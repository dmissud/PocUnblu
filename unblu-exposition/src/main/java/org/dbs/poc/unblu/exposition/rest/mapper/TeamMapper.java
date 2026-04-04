package org.dbs.poc.unblu.exposition.rest.mapper;

import org.apache.camel.Exchange;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.domain.port.in.SearchTeamsUseCase;
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

    private final SearchTeamsUseCase searchTeamsUseCase;

    public TeamMapper(SearchTeamsUseCase searchTeamsUseCase) {
        this.searchTeamsUseCase = searchTeamsUseCase;
    }

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
     * Searches teams and directly returns TeamResponse list.
     * This method combines query execution and response mapping.
     */
    public void searchAndMapTeams(Exchange exchange) {
        List<TeamInfo> teamInfos = searchTeamsUseCase.searchTeams();
        exchange.getIn().setBody(toResponseList(teamInfos));
    }
}
