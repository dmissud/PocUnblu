package org.dbs.poc.unblu.infrastructure.orchestration.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartConversationResponse {
    private String unbluConversationId;
    private String unbluJoinUrl;
    private String status;
    private String message;
}
