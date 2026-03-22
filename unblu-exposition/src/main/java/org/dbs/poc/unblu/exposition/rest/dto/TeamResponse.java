package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de réponse REST représentant une équipe (file d'agents) Unblu.
 * Retourné par {@code GET /api/v1/teams}.
 */
@Data
@Builder
public class TeamResponse {
    private String id;
    private String name;
    private String description;
}
