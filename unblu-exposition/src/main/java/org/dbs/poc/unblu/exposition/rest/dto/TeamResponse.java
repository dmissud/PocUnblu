package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamResponse {
    private String id;
    private String name;
    private String description;
}
