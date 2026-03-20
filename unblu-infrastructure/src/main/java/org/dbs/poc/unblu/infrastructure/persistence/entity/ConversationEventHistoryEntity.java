package org.dbs.poc.unblu.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity for conversation event.
 */
@Entity
@Table(name = "conversation_event_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationEventHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationHistoryEntity conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "message_text", columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "sender_person_id")
    private String senderPersonId;

    @Column(name = "sender_display_name")
    private String senderDisplayName;

    public enum EventType {
        CREATED,
        MESSAGE,
        ENDED
    }
}
