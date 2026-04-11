package org.dbs.poc.unblu.engine.rest.dto;

import lombok.Builder;

@Builder
public record ConversationEventResponse(
        String eventType,
        String occurredAt,
        String messageText,
        String senderPersonId,
        String senderDisplayName) {
}
