package org.dbs.poc.unblu.exposition.rest.mapper;

import org.apache.camel.Exchange;
import org.dbs.poc.unblu.application.model.ConversationOrchestrationState;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationRequest;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationResponse;
import org.dbs.poc.unblu.exposition.rest.dto.StartDirectConversationRequest;
import org.springframework.stereotype.Component;

/**
 * Mapper for conversation-related DTO transformations.
 * Handles conversion between REST DTOs and domain commands/models.
 */
@Component
public class ConversationMapper {

    private static final String STATUS_CREATED = "CREATED";
    private static final String MESSAGE_TEAM_CONVERSATION_CREATED = "Conversation successfully created.";
    private static final String MESSAGE_DIRECT_CONVERSATION_CREATED = "Conversation directe créée avec succès.";

    private final String defaultTeamId;

    public ConversationMapper(String defaultTeamId) {
        this.defaultTeamId = defaultTeamId;
    }

    /**
     * Maps a StartConversationRequest to a StartConversationCommand.
     */
    public StartConversationCommand toCommand(StartConversationRequest request) {
        return new StartConversationCommand(
                request.getClientId(),
                request.getSubject(),
                request.getOrigin(),
                defaultTeamId
        );
    }

    /**
     * Maps a StartDirectConversationRequest to a StartDirectConversationCommand.
     */
    public StartDirectConversationCommand toCommand(StartDirectConversationRequest request) {
        return new StartDirectConversationCommand(
                request.getVirtualParticipantSourceId(),
                request.getAgentParticipantSourceId(),
                request.getSubject()
        );
    }

    /**
     * Maps a ConversationOrchestrationState to a StartConversationResponse.
     */
    public StartConversationResponse toResponse(ConversationOrchestrationState state) {
        return StartConversationResponse.builder()
                .unbluConversationId(state.unbluConversationId())
                .unbluJoinUrl(state.unbluJoinUrl())
                .status(STATUS_CREATED)
                .message(MESSAGE_TEAM_CONVERSATION_CREATED)
                .build();
    }

    /**
     * Maps an UnbluConversationInfo to a StartConversationResponse.
     */
    public StartConversationResponse toResponse(UnbluConversationInfo info) {
        return StartConversationResponse.builder()
                .unbluConversationId(info.unbluConversationId())
                .unbluJoinUrl(info.unbluJoinUrl())
                .status(STATUS_CREATED)
                .message(MESSAGE_DIRECT_CONVERSATION_CREATED)
                .build();
    }

    /**
     * Extracts and maps StartConversationRequest from Camel Exchange to Command.
     */
    public void mapRequestToCommand(Exchange exchange) {
        StartConversationRequest request = exchange.getIn().getBody(StartConversationRequest.class);
        exchange.getIn().setBody(toCommand(request));
    }

    /**
     * Extracts and maps StartDirectConversationRequest from Camel Exchange to Command.
     */
    public void mapDirectRequestToCommand(Exchange exchange) {
        StartDirectConversationRequest request = exchange.getIn().getBody(StartDirectConversationRequest.class);
        exchange.getIn().setBody(toCommand(request));
    }

    /**
     * Extracts and maps ConversationOrchestrationState from Camel Exchange to Response.
     */
    public void mapContextToResponse(Exchange exchange) {
        ConversationOrchestrationState state = exchange.getIn().getBody(ConversationOrchestrationState.class);
        exchange.getIn().setBody(toResponse(state));
    }

    /**
     * Extracts and maps UnbluConversationInfo from Camel Exchange to Response.
     */
    public void mapInfoToResponse(Exchange exchange) {
        UnbluConversationInfo info = exchange.getIn().getBody(UnbluConversationInfo.class);
        exchange.getIn().setBody(toResponse(info));
    }
}
