package org.dbs.poc.unblu.eventprocessor.rest.dto;

import lombok.Builder;

@Builder
public record ConversationEventResponse(
        String eventType,
        String occurredAt,
        String messageText,
        String senderPersonId,
        String senderDisplayName) {
}
