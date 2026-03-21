package org.dbs.poc.unblu.domain.model.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Value object representing a single event that occurred during a conversation.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEventHistory {

    private EventType eventType;
    private Instant eventTime;

    // For MESSAGE events
    private String messageText;
    private String senderPersonId;
    private String senderDisplayName;

    public enum EventType {
        CREATED,
        MESSAGE,
        ENDED
    }

    // --- Accessors ---

    public EventType eventType() { return eventType; }

    public Instant occurredAt() { return eventTime; }

    public String messageText() { return messageText; }

    public String senderPersonId() { return senderPersonId; }

    public String senderDisplayName() { return senderDisplayName; }
}
