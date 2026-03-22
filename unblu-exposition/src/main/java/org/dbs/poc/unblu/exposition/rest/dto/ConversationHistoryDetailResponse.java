package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * DTO représentant le détail complet d'une conversation :
 * métadonnées, participants et événements dans l'ordre chronologique.
 */
@Value
@Builder
public class ConversationHistoryDetailResponse {

    String conversationId;
    String topic;
    Instant createdAt;
    Instant endedAt;
    String status;
    List<ConversationParticipantResponse> participants;
    /** Événements triés par ordre chronologique croissant. */
    List<ConversationEventResponse> events;
}
