package org.dbs.poc.unblu.exposition.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartDirectConversationRequest {
    @NotBlank
    private String virtualParticipantSourceId;
    @NotBlank
    private String agentParticipantSourceId;
    @NotBlank
    private String subject;
}
