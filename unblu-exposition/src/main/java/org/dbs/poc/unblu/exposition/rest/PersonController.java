package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SearchPersonsQuery;
import org.dbs.poc.unblu.application.port.in.SearchPersonsUseCase;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.exposition.rest.dto.PersonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/persons")
@RequiredArgsConstructor
public class PersonController {

    private final SearchPersonsUseCase searchPersonsUseCase;

    @GetMapping
    public ResponseEntity<List<PersonResponse>> searchPersons(
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) PersonSource personSource) {

        List<PersonResponse> response = searchPersonsUseCase
                .searchPersons(new SearchPersonsQuery(sourceId, personSource))
                .stream()
                .map(personInfo -> PersonResponse.builder()
                        .id(personInfo.id())
                        .sourceId(personInfo.sourceId())
                        .displayName(personInfo.displayName())
                        .email(personInfo.email())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }
}
