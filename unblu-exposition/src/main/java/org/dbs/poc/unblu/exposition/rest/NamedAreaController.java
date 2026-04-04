package org.dbs.poc.unblu.exposition.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.domain.port.in.SearchAgentsByNamedAreaUseCase;
import org.dbs.poc.unblu.domain.port.in.SearchNamedAreasUseCase;
import org.dbs.poc.unblu.exposition.rest.dto.NamedAreaResponse;
import org.dbs.poc.unblu.exposition.rest.dto.PersonResponse;
import org.dbs.poc.unblu.exposition.rest.mapper.NamedAreaMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/named-areas")
@RequiredArgsConstructor
@Tag(name = "Named Areas", description = "Gestion des zones nommées Unblu")
public class NamedAreaController {

    private final SearchNamedAreasUseCase searchNamedAreasUseCase;
    private final SearchAgentsByNamedAreaUseCase searchAgentsByNamedAreaUseCase;
    private final NamedAreaMapper namedAreaMapper;

    @GetMapping
    @Operation(summary = "Liste toutes les zones nommées")
    public ResponseEntity<List<NamedAreaResponse>> list() {
        var areas = searchNamedAreasUseCase.searchNamedAreas();
        return ResponseEntity.ok(namedAreaMapper.toResponseList(areas));
    }

    @GetMapping("/{namedAreaId}/agents")
    @Operation(summary = "Liste les agents d'une zone nommée")
    public ResponseEntity<List<PersonResponse>> listAgents(@PathVariable String namedAreaId) {
        var agents = searchAgentsByNamedAreaUseCase.searchAgentsByNamedArea(namedAreaId);
        // On réutilise le mapping de NamedAreaMapper pour rester cohérent avec l'existant
        List<PersonResponse> responses = agents.stream()
                .map(agent -> PersonResponse.builder()
                        .id(agent.id())
                        .sourceId(agent.sourceId())
                        .displayName(agent.displayName())
                        .email(agent.email())
                        .build())
                .toList();
        return ResponseEntity.ok(responses);
    }
}
