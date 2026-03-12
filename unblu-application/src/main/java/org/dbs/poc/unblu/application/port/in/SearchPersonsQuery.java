package org.dbs.poc.unblu.application.port.in;

import lombok.Builder;
import lombok.Data;
import org.dbs.poc.unblu.domain.model.PersonSource;

@Data
@Builder
public class SearchPersonsQuery {
    private String sourceId;
    private PersonSource personSource;
}
