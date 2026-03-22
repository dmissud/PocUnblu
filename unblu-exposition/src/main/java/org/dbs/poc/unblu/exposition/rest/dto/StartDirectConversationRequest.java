package org.dbs.poc.unblu.exposition.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de requête REST pour démarrer une conversation directe (1-à-1) entre un participant
 * virtuel et un agent Unblu.
 * Correspond au body du {@code POST /api/v1/conversations/direct}.
 */
@Data
public class StartDirectConversationRequest {
    @NotBlank
    private String virtualParticipantSourceId;
    @NotBlank
    private String agentParticipantSourceId;
    @NotBlank
    private String subject;
}
