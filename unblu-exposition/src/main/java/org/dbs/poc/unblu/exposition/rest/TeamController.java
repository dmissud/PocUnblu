package org.dbs.poc.unblu.exposition.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.domain.port.in.SearchTeamsUseCase;
import org.dbs.poc.unblu.exposition.rest.dto.TeamResponse;
import org.dbs.poc.unblu.exposition.rest.mapper.TeamMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Gestion des équipes Unblu")
public class TeamController {

    private final SearchTeamsUseCase searchTeamsUseCase;
    private final TeamMapper teamMapper;

    @GetMapping
    @Operation(summary = "Liste toutes les équipes")
    public ResponseEntity<List<TeamResponse>> list() {
        var teams = searchTeamsUseCase.searchTeams();
        return ResponseEntity.ok(teamMapper.toResponseList(teams));
    }
}
