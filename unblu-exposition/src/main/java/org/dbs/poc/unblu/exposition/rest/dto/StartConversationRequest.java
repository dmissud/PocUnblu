package org.dbs.poc.unblu.exposition.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de requête REST pour démarrer une conversation Unblu avec une équipe.
 * Correspond au body du {@code POST /api/v1/conversations/start}.
 */
@Data
public class StartConversationRequest {
    @NotBlank(message = "clientId is required")
    private String clientId;
    @NotBlank(message = "subject is required")
    private String subject;
    private String origin;
}
