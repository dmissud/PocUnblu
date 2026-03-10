package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SearchTeamsUseCase;
import org.dbs.poc.unblu.exposition.rest.dto.TeamResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final SearchTeamsUseCase searchTeamsUseCase;

    @GetMapping
    public ResponseEntity<List<TeamResponse>> searchTeams() {
        List<TeamResponse> response = searchTeamsUseCase.searchTeams()
                .stream()
                .map(t -> TeamResponse.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .description(t.getDescription())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }
}
