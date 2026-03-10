package org.dbs.poc.unblu.application.port.in;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchPersonsQuery {
    private String sourceId;
}
