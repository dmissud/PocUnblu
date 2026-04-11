package org.dbs.poc.unblu.integration.infrastructure.persistence.mapper;

import org.dbs.poc.unblu.integration.domain.model.history.ConversationEventHistory;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.history.ParticipantHistory;
import org.dbs.poc.unblu.integration.infrastructure.persistence.entity.ConversationEventHistoryEntity;
import org.dbs.poc.unblu.integration.infrastructure.persistence.entity.ConversationHistoryEntity;
import org.dbs.poc.unblu.integration.infrastructure.persistence.entity.ParticipantHistoryEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ConversationHistoryMapper {

    public ConversationHistory toDomainSummary(ConversationHistoryEntity entity) {
        if (entity == null) return null;
        return ConversationHistory.builder()
                .conversationId(entity.getConversationId())
                .topic(entity.getTopic())
                .createdAt(entity.getCreatedAt())
                .endedAt(entity.getEndedAt())
                .build();
    }

    public ConversationHistoryEntity toEntity(ConversationHistory domain) {
        if (domain == null) return null;
        ConversationHistoryEntity entity = ConversationHistoryEntity.builder()
                .conversationId(domain.conversationId())
                .topic(domain.topic())
                .createdAt(domain.startedAt())
                .endedAt(domain.endedAt())
                .build();
        domain.participants().forEach(p -> entity.addParticipant(toParticipantEntity(p)));
        domain.events().forEach(e -> entity.addEvent(toEventEntity(e)));
        return entity;
    }

    public ConversationHistory toDomain(ConversationHistoryEntity entity) {
        if (entity == null) return null;
        return ConversationHistory.builder()
                .conversationId(entity.getConversationId())
                .topic(entity.getTopic())
                .createdAt(entity.getCreatedAt())
                .endedAt(entity.getEndedAt())
                .participants(entity.getParticipants().stream().map(this::toParticipantDomain).collect(Collectors.toList()))
                .events(entity.getEvents().stream().map(this::toEventDomain).collect(Collectors.toList()))
                .build();
    }

    private ParticipantHistoryEntity toParticipantEntity(ParticipantHistory domain) {
        return ParticipantHistoryEntity.builder()
                .personId(domain.personId())
                .displayName(domain.displayName())
                .type(ParticipantHistoryEntity.ParticipantType.valueOf(domain.participantType().name()))
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
                .eventType(ConversationEventHistoryEntity.EventType.valueOf(domain.eventType().name()))
                .eventTime(domain.occurredAt())
                .messageId(domain.messageId())
                .messageText(domain.messageText())
                .senderPersonId(domain.senderPersonId())
                .senderDisplayName(domain.senderDisplayName())
                .build();
    }

    private ConversationEventHistory toEventDomain(ConversationEventHistoryEntity entity) {
        return ConversationEventHistory.builder()
                .eventType(ConversationEventHistory.EventType.valueOf(entity.getEventType().name()))
                .eventTime(entity.getEventTime())
                .messageId(entity.getMessageId())
                .messageText(entity.getMessageText())
                .senderPersonId(entity.getSenderPersonId())
                .senderDisplayName(entity.getSenderDisplayName())
                .build();
    }
}
