package org.dbs.poc.unblu.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity for conversation participant.
 */
@Entity
@Table(name = "participant_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationHistoryEntity conversation;

    @Column(name = "person_id", nullable = false)
    private String personId;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ParticipantType type;

    public enum ParticipantType {
        VISITOR,
        AGENT,
        BOT
    }
}
