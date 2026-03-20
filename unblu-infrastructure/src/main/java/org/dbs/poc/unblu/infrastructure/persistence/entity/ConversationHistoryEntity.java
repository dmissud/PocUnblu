package org.dbs.poc.unblu.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for conversation history.
 */
@Entity
@Table(name = "conversation_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, unique = true)
    private String conversationId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParticipantHistoryEntity> participants = new ArrayList<>();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConversationEventHistoryEntity> events = new ArrayList<>();

    public void addParticipant(ParticipantHistoryEntity participant) {
        participants.add(participant);
        participant.setConversation(this);
    }

    public void addEvent(ConversationEventHistoryEntity event) {
        events.add(event);
        event.setConversation(this);
    }
}
