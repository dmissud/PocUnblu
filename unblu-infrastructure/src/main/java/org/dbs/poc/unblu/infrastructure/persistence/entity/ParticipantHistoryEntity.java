package org.dbs.poc.unblu.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entité JPA représentant un participant dans l'historique d'une conversation Unblu.
 * Un participant peut être un visiteur, un agent ou un bot.
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

    /**
     * Type de participant dans une conversation.
     */
    public enum ParticipantType {
        /**
         * Visiteur ou personne virtuelle initiant la conversation.
         */
        VISITOR,
        /** Agent Unblu traitant la conversation. */
        AGENT,
        /** Bot automatisé participant à la conversation (ex. bot de résumé). */
        BOT
    }
}
