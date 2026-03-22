package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * DTO représentant le détail complet d'une conversation :
 * métadonnées, participants et événements dans l'ordre chronologique.
 *
 * <p>Les horodatages sont sérialisés en ISO 8601 (String).
 */
@Value
@Builder
public class ConversationHistoryDetailResponse {

    String conversationId;
    String topic;
    /** Horodatage ISO 8601. */
    String createdAt;
    /** Horodatage ISO 8601, {@code null} si la conversation est encore active. */
    String endedAt;
    String status;
    List<ConversationParticipantResponse> participants;
    /** Événements triés par ordre chronologique croissant. */
    List<ConversationEventResponse> events;
}
