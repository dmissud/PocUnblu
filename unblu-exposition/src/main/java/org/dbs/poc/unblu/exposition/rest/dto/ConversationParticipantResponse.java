package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Value;

/**
 * DTO représentant un participant d'une conversation.
 */
@Value
@Builder
public class ConversationParticipantResponse {

    String personId;
    String displayName;
    /** {@code VISITOR}, {@code AGENT} ou {@code BOT}. */
    String participantType;
}
