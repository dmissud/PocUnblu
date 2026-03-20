package org.dbs.poc.unblu.domain.model.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a conversation history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistory {

    private String conversationId;
    private String topic;
    private Instant createdAt;
    private Instant endedAt;

    @Builder.Default
    private List<ParticipantHistory> participants = new ArrayList<>();

    @Builder.Default
    private List<ConversationEventHistory> events = new ArrayList<>();

    public void addParticipant(ParticipantHistory participant) {
        this.participants.add(participant);
    }

    public void addEvent(ConversationEventHistory event) {
        this.events.add(event);
    }
}
