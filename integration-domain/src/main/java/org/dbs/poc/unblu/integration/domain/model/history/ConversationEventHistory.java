package org.dbs.poc.unblu.integration.domain.model.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEventHistory {
    private EventType eventType;
    private Instant eventTime;
    private String messageId;
    private String messageText;
    private String senderPersonId;
    private String senderDisplayName;

    public EventType eventType() { return eventType; }
    public Instant occurredAt() { return eventTime; }
    public String messageId() { return messageId; }
    public String messageText() { return messageText; }
    public String senderPersonId() { return senderPersonId; }
    public String senderDisplayName() { return senderDisplayName; }

    public enum EventType { CREATED, MESSAGE, ENDED }
}
