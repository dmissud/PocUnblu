package org.dbs.poc.unblu.exposition.rest.mapper;

import org.dbs.poc.unblu.application.port.in.command.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.command.StartDirectConversationCommand;
import org.dbs.poc.unblu.domain.model.ConversationOrchestrationState;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationRequest;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationResponse;
import org.dbs.poc.unblu.exposition.rest.dto.StartDirectConversationRequest;

/**
 * Mapper for conversation-related DTO transformations.
 * Handles conversion between REST DTOs and domain commands/models.
 */
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
                request.getVirtualParticipantId(),
                request.getVirtualParticipantSourceId(),
                request.getAgentParticipantId(),
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
}
