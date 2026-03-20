package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PersonResponse {
    private String id;
    private String sourceId;
    private String displayName;
    private String email;
    private String firstName;
    private String lastName;
    private String personType;
}
