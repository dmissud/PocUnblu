package org.dbs.poc.unblu.exposition.rest.mapper;

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
                .personType(personInfo.personType())
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

    public PersonSource parsePersonSource(String personSourceStr) {
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
