package org.dbs.poc.unblu.infrastructure.orchestration.dto;

import lombok.Data;

@Data
public class StartConversationRequest {
    private String clientId;
    private String subject;
    private String origin;
}
