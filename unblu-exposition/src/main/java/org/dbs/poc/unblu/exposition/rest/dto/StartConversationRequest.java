package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Data;

/**
 * DTO de requête REST pour démarrer une conversation Unblu avec une équipe.
 * Correspond au body du {@code POST /api/v1/conversations/start}.
 */
@Data
public class StartConversationRequest {
    private String clientId;
    private String subject;
    private String origin;
}
