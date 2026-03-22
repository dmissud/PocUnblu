package org.dbs.poc.unblu.exposition.rest.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * DTO représentant un événement de la timeline d'une conversation.
 */
@Value
@Builder
public class ConversationEventResponse {

    /** {@code CREATED}, {@code MESSAGE} ou {@code ENDED}. */
    String eventType;
    Instant occurredAt;
    String messageText;
    String senderPersonId;
    String senderDisplayName;
}
