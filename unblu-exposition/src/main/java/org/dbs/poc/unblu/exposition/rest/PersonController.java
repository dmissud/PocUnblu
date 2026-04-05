package org.dbs.poc.unblu.exposition.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SearchPersonsUseCase;
import org.dbs.poc.unblu.application.port.in.query.SearchPersonsQuery;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.exposition.rest.dto.PersonResponse;
import org.dbs.poc.unblu.exposition.rest.mapper.PersonMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@Tag(name = "Persons", description = "Gestion des personnes Unblu")
public class PersonController {

    private final SearchPersonsUseCase searchPersonsUseCase;
    private final PersonMapper personMapper;

    @GetMapping
    @Operation(summary = "Recherche des personnes")
    public ResponseEntity<List<PersonResponse>> search(
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String personSource) {

        PersonSource source = personMapper.parsePersonSource(personSource);
        var query = new SearchPersonsQuery(sourceId, source);
        var persons = searchPersonsUseCase.searchPersons(query);
        return ResponseEntity.ok(personMapper.toResponseList(persons));
    }
}
