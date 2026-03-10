package org.dbs.poc.unblu.application.port.in;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartConversationCommand {
    private String clientId;
    private String subject;
    private String origin;
}
