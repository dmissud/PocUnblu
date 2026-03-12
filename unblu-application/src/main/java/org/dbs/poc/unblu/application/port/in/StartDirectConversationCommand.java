package org.dbs.poc.unblu.application.port.in;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartDirectConversationCommand {
    private String virtualParticipantSourceId;
    private String agentParticipantSourceId;
    private String subject;
}
