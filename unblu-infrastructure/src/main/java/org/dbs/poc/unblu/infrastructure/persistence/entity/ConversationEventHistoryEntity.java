package org.dbs.poc.unblu.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entité JPA représentant un événement dans l'historique d'une conversation Unblu.
 * Peut être un événement de création ({@link EventType#CREATED}), un message ({@link EventType#MESSAGE})
 * ou une fin de conversation ({@link EventType#ENDED}).
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

    /**
     * Type d'événement de conversation persisté en base.
     */
    public enum EventType {
        /**
         * La conversation a été créée.
         */
        CREATED,
        /** Un message a été envoyé dans la conversation. */
        MESSAGE,
        /** La conversation a été terminée. */
        ENDED
    }
}
