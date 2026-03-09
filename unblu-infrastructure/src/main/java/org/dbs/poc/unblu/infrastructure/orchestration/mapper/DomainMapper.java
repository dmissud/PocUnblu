package org.dbs.poc.unblu.infrastructure.orchestration.mapper;

import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.infrastructure.orchestration.dto.StartConversationRequest;
import org.dbs.poc.unblu.infrastructure.orchestration.dto.StartConversationResponse;
import org.springframework.stereotype.Component;

@Component
public class DomainMapper {

    public ConversationContext initContext(StartConversationRequest request) {
        return ConversationContext.builder()
                .initialClientId(request.getClientId())
                .originApplication(request.getOrigin())
                .build();
    }

    public StartConversationResponse toFrontendResponse(ConversationContext context) {
        return StartConversationResponse.builder()
                .unbluConversationId(context.getUnbluConversationId())
                .unbluJoinUrl(context.getUnbluJoinUrl())
                .status("CREATED")
                .message("Conversation successfully created.")
                .build();
    }
}
