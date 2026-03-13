package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Data;

@Data
public class StartConversationRequest {
    private String clientId;
    private String subject;
    private String origin;
}
