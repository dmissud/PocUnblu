package org.dbs.poc.unblu.exposition.rest.mapper;

import org.apache.camel.Exchange;
import org.dbs.poc.unblu.application.port.in.SearchPersonsQuery;
import org.dbs.poc.unblu.application.port.in.SearchPersonsUseCase;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.exposition.rest.dto.PersonResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper for person-related DTO transformations.
 * Handles conversion between REST DTOs and domain queries/models.
 */
@Component
public class PersonMapper {

    private static final String HEADER_SOURCE_ID = "sourceId";
    private static final String HEADER_PERSON_SOURCE = "personSource";

    private final SearchPersonsUseCase searchPersonsUseCase;

    public PersonMapper(SearchPersonsUseCase searchPersonsUseCase) {
        this.searchPersonsUseCase = searchPersonsUseCase;
    }

    /**
     * Maps a PersonInfo domain model to a PersonResponse DTO.
     */
    public PersonResponse toResponse(PersonInfo personInfo) {
        return PersonResponse.builder()
                .id(personInfo.id())
                .sourceId(personInfo.sourceId())
                .displayName(personInfo.displayName())
                .email(personInfo.email())
                .firstName(personInfo.firstName())
                .lastName(personInfo.lastName())
                .build();
    }

    /**
     * Maps a list of PersonInfo to a list of PersonResponse.
     */
    public List<PersonResponse> toResponseList(List<PersonInfo> personInfos) {
        if (personInfos == null) {
            return Collections.emptyList();
        }

        return personInfos.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Searches persons using headers and directly returns PersonResponse list.
     * This method combines query execution and response mapping.
     */
    public void searchAndMapPersons(Exchange exchange) {
        String sourceId = exchange.getIn().getHeader(HEADER_SOURCE_ID, String.class);
        String personSourceStr = exchange.getIn().getHeader(HEADER_PERSON_SOURCE, String.class);
        PersonSource personSource = parsePersonSource(personSourceStr);

        SearchPersonsQuery query = new SearchPersonsQuery(sourceId, personSource);
        List<PersonInfo> personInfos = searchPersonsUseCase.searchPersons(query);

        exchange.getIn().setBody(toResponseList(personInfos));
    }

    private PersonSource parsePersonSource(String personSourceStr) {
        if (personSourceStr == null || personSourceStr.isBlank()) {
            return null;
        }

        try {
            return PersonSource.valueOf(personSourceStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
