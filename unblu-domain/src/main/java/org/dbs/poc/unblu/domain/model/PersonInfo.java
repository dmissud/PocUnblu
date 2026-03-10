package org.dbs.poc.unblu.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonInfo {
    private String id;
    private String sourceId;
    private String displayName;
    private String email;
}
