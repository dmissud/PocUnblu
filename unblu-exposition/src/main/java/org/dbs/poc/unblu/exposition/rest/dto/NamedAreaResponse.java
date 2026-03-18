package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NamedAreaResponse {
    private String id;
    private String name;
    private String description;
}
