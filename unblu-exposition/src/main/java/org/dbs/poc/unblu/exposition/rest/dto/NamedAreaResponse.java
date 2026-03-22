package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de réponse REST représentant une zone nommée Unblu.
 * Retourné par {@code GET /api/v1/named-areas}.
 */
@Data
@Builder
public class NamedAreaResponse {
    private String id;
    private String name;
    private String description;
}
