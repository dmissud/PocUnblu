package org.dbs.poc.unblu.infrastructure.persistence.mapper;

import org.dbs.poc.unblu.domain.model.history.ConversationEventHistory;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.model.history.ParticipantHistory;
import org.dbs.poc.unblu.infrastructure.persistence.entity.ConversationEventHistoryEntity;
import org.dbs.poc.unblu.infrastructure.persistence.entity.ConversationHistoryEntity;
import org.dbs.poc.unblu.infrastructure.persistence.entity.ParticipantHistoryEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper de conversion entre les objets du domaine ({@link ConversationHistory}, {@link ParticipantHistory},
 * {@link ConversationEventHistory}) et les entités JPA correspondantes.
 */
@Component
public class ConversationHistoryMapper {

    /**
     * Convertit un objet domaine {@link ConversationHistory} en entité JPA, incluant
     * les participants et les événements associés.
     *
     * @param domain l'objet domaine à convertir (peut être {@code null})
     * @return l'entité JPA correspondante, ou {@code null} si {@code domain} est {@code null}
     */
    public ConversationHistoryEntity toEntity(ConversationHistory domain) {
        if (domain == null) {
            return null;
        }

        ConversationHistoryEntity entity = ConversationHistoryEntity.builder()
                .conversationId(domain.conversationId())
                .topic(domain.topic())
                .createdAt(domain.startedAt())
                .endedAt(domain.endedAt())
                .build();

        domain.participants().forEach(participant ->
                entity.addParticipant(toParticipantEntity(participant)));

        domain.events().forEach(event ->
                entity.addEvent(toEventEntity(event)));

        return entity;
    }

    /**
     * Convertit une entité JPA {@link ConversationHistoryEntity} en objet domaine, incluant
     * les participants et les événements associés.
     *
     * @param entity l'entité JPA à convertir (peut être {@code null})
     * @return l'objet domaine correspondant, ou {@code null} si {@code entity} est {@code null}
     */
    public ConversationHistory toDomain(ConversationHistoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return ConversationHistory.builder()
                .conversationId(entity.getConversationId())
                .topic(entity.getTopic())
                .createdAt(entity.getCreatedAt())
                .endedAt(entity.getEndedAt())
                .participants(entity.getParticipants().stream()
                        .map(this::toParticipantDomain)
                        .collect(Collectors.toList()))
                .events(entity.getEvents().stream()
                        .map(this::toEventDomain)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Convertit un {@link ParticipantHistory} domaine en entité JPA {@link ParticipantHistoryEntity}.
     *
     * @param domain le participant domaine
     * @return l'entité JPA correspondante
     */
    private ParticipantHistoryEntity toParticipantEntity(ParticipantHistory domain) {
        return ParticipantHistoryEntity.builder()
                .personId(domain.personId())
                .displayName(domain.displayName())
                .type(ParticipantHistoryEntity.ParticipantType.valueOf(domain.participantType().name()))
                .build();
    }

    /**
     * Convertit une entité JPA {@link ParticipantHistoryEntity} en objet domaine {@link ParticipantHistory}.
     *
     * @param entity l'entité JPA participant
     * @return l'objet domaine correspondant
     */
    private ParticipantHistory toParticipantDomain(ParticipantHistoryEntity entity) {
        return ParticipantHistory.builder()
                .personId(entity.getPersonId())
                .displayName(entity.getDisplayName())
                .type(ParticipantHistory.ParticipantType.valueOf(entity.getType().name()))
                .build();
    }

    /**
     * Convertit un {@link ConversationEventHistory} domaine en entité JPA {@link ConversationEventHistoryEntity}.
     *
     * @param domain l'événement domaine
     * @return l'entité JPA correspondante
     */
    private ConversationEventHistoryEntity toEventEntity(ConversationEventHistory domain) {
        return ConversationEventHistoryEntity.builder()
                .eventType(ConversationEventHistoryEntity.EventType.valueOf(domain.eventType().name()))
                .eventTime(domain.occurredAt())
                .messageText(domain.messageText())
                .senderPersonId(domain.senderPersonId())
                .senderDisplayName(domain.senderDisplayName())
                .build();
    }

    /**
     * Convertit une entité JPA {@link ConversationEventHistoryEntity} en objet domaine {@link ConversationEventHistory}.
     *
     * @param entity l'entité JPA événement
     * @return l'objet domaine correspondant
     */
    private ConversationEventHistory toEventDomain(ConversationEventHistoryEntity entity) {
        return ConversationEventHistory.builder()
                .eventType(ConversationEventHistory.EventType.valueOf(entity.getEventType().name()))
                .eventTime(entity.getEventTime())
                .messageText(entity.getMessageText())
                .senderPersonId(entity.getSenderPersonId())
                .senderDisplayName(entity.getSenderDisplayName())
                .build();
    }
}
