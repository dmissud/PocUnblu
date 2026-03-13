package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SearchTeamsUseCase;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rest/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final SearchTeamsUseCase searchTeamsUseCase;

    @GetMapping
    public ResponseEntity<List<TeamInfo>> getTeams() {
        List<TeamInfo> teams = searchTeamsUseCase.searchTeams();
        return ResponseEntity.ok(teams);
    }
}
