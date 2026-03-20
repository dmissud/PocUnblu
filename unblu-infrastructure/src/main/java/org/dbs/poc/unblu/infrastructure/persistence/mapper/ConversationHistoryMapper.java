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
 * Mapper between domain and JPA entities for conversation history.
 */
@Component
public class ConversationHistoryMapper {

    public ConversationHistoryEntity toEntity(ConversationHistory domain) {
        if (domain == null) {
            return null;
        }

        ConversationHistoryEntity entity = ConversationHistoryEntity.builder()
                .conversationId(domain.getConversationId())
                .topic(domain.getTopic())
                .createdAt(domain.getCreatedAt())
                .endedAt(domain.getEndedAt())
                .build();

        // Map participants
        if (domain.getParticipants() != null) {
            domain.getParticipants().forEach(participant -> {
                ParticipantHistoryEntity participantEntity = toParticipantEntity(participant);
                entity.addParticipant(participantEntity);
            });
        }

        // Map events
        if (domain.getEvents() != null) {
            domain.getEvents().forEach(event -> {
                ConversationEventHistoryEntity eventEntity = toEventEntity(event);
                entity.addEvent(eventEntity);
            });
        }

        return entity;
    }

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

    private ParticipantHistoryEntity toParticipantEntity(ParticipantHistory domain) {
        return ParticipantHistoryEntity.builder()
                .personId(domain.getPersonId())
                .displayName(domain.getDisplayName())
                .type(ParticipantHistoryEntity.ParticipantType.valueOf(domain.getType().name()))
                .build();
    }

    private ParticipantHistory toParticipantDomain(ParticipantHistoryEntity entity) {
        return ParticipantHistory.builder()
                .personId(entity.getPersonId())
                .displayName(entity.getDisplayName())
                .type(ParticipantHistory.ParticipantType.valueOf(entity.getType().name()))
                .build();
    }

    private ConversationEventHistoryEntity toEventEntity(ConversationEventHistory domain) {
        return ConversationEventHistoryEntity.builder()
                .eventType(ConversationEventHistoryEntity.EventType.valueOf(domain.getEventType().name()))
                .eventTime(domain.getEventTime())
                .messageText(domain.getMessageText())
                .senderPersonId(domain.getSenderPersonId())
                .senderDisplayName(domain.getSenderDisplayName())
                .build();
    }

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
