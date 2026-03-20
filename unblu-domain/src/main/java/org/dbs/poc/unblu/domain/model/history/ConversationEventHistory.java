package org.dbs.poc.unblu.domain.model.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain entity representing a conversation event.
 */
@Data
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
}
