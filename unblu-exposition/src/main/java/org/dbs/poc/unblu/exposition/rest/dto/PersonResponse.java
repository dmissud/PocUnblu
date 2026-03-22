package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de réponse REST représentant une personne Unblu (agent ou participant virtuel).
 * Retourné par {@code GET /api/v1/persons}.
 */
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
