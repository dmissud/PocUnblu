package org.dbs.poc.unblu.exposition.rest;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.application.port.in.SearchPersonsQuery;
import org.dbs.poc.unblu.application.port.in.SearchPersonsUseCase;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rest/v1/persons")
@RequiredArgsConstructor
public class PersonController {

    private final SearchPersonsUseCase searchPersonsUseCase;

    @GetMapping("/clients")
    public ResponseEntity<List<PersonInfo>> getClients() {
        List<PersonInfo> clients = searchPersonsUseCase.searchPersons(
                new SearchPersonsQuery(null, PersonSource.VIRTUAL)
        );
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/agents")
    public ResponseEntity<List<PersonInfo>> getAgents() {
        List<PersonInfo> agents = searchPersonsUseCase.searchPersons(
                new SearchPersonsQuery(null, PersonSource.USER_DB)
        );
        return ResponseEntity.ok(agents);
    }
}
