package org.dbs.poc.unblu.domain.model.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain entity representing the history of a conversation.
 *
 * <p>This is a rich entity — state transitions are enforced via dedicated methods.
 * Direct field mutation is intentionally not exposed (no setters, no getXxx accessors).
 *
 * <p>Infrastructure reconstruction (e.g. from the database) uses the Lombok {@code @Builder}.
 */
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

    // --- Factory ---

    /**
     * Creates a new conversation history in the initial CREATED state.
     * The CREATED event is recorded automatically.
     */
    public static ConversationHistory create(String conversationId, String topic, Instant startedAt) {
        ConversationHistory history = ConversationHistory.builder()
                .conversationId(conversationId)
                .topic(topic)
                .createdAt(startedAt)
                .build();
        history.events.add(ConversationEventHistory.builder()
                .eventType(ConversationEventHistory.EventType.CREATED)
                .eventTime(startedAt)
                .build());
        return history;
    }

    // --- Business methods ---

    /**
     * Records a new message in this conversation.
     *
     * @throws IllegalStateException if the conversation has already ended
     */
    public void recordMessage(String messageText, String senderPersonId, String senderDisplayName, Instant time) {
        if (isEnded()) {
            throw new IllegalStateException("Cannot record message on ended conversation: " + conversationId);
        }
        this.events.add(ConversationEventHistory.builder()
                .eventType(ConversationEventHistory.EventType.MESSAGE)
                .eventTime(time)
                .messageText(messageText)
                .senderPersonId(senderPersonId)
                .senderDisplayName(senderDisplayName)
                .build());
    }

    /**
     * Ends this conversation and records the ENDED event.
     *
     * @throws IllegalStateException if the conversation has already ended
     */
    public void end(Instant endedAt) {
        if (isEnded()) {
            throw new IllegalStateException("Conversation already ended: " + conversationId);
        }
        this.endedAt = endedAt;
        this.events.add(ConversationEventHistory.builder()
                .eventType(ConversationEventHistory.EventType.ENDED)
                .eventTime(endedAt)
                .build());
    }

    /**
     * Registers a participant if not already present (idempotent).
     */
    public void registerParticipant(String personId, String displayName, ParticipantHistory.ParticipantType type) {
        boolean alreadyPresent = this.participants.stream()
                .anyMatch(p -> p.personId().equals(personId));
        if (!alreadyPresent) {
            this.participants.add(ParticipantHistory.builder()
                    .personId(personId)
                    .displayName(displayName)
                    .type(type)
                    .build());
        }
    }

    // --- Accessors (intent-revealing, no getXxx prefix) ---

    public String conversationId() { return conversationId; }

    public String topic() { return topic; }

    public Instant startedAt() { return createdAt; }

    public Instant endedAt() { return endedAt; }

    public boolean isEnded() { return endedAt != null; }

    public List<ParticipantHistory> participants() { return Collections.unmodifiableList(participants); }

    public List<ConversationEventHistory> events() { return Collections.unmodifiableList(events); }
}
