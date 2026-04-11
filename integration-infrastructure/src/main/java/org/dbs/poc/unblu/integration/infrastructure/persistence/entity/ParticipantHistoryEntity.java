package org.dbs.poc.unblu.integration.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "participant_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParticipantHistoryEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationHistoryEntity conversation;

    @Column(name = "person_id")
    private String personId;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ParticipantType type;

    public enum ParticipantType { VISITOR, AGENT, BOT }
}
