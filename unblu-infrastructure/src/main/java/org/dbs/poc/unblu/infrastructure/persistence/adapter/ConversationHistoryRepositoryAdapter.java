package org.dbs.poc.unblu.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.infrastructure.persistence.entity.ConversationEventHistoryEntity;
import org.dbs.poc.unblu.infrastructure.persistence.entity.ConversationHistoryEntity;
import org.dbs.poc.unblu.infrastructure.persistence.entity.ParticipantHistoryEntity;
import org.dbs.poc.unblu.infrastructure.persistence.mapper.ConversationHistoryMapper;
import org.dbs.poc.unblu.infrastructure.persistence.repository.ConversationHistoryJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter implementing the ConversationHistoryRepository port using JPA.
 */
@Component
@RequiredArgsConstructor
public class ConversationHistoryRepositoryAdapter implements ConversationHistoryRepository {

    private final ConversationHistoryJpaRepository jpaRepository;
    private final ConversationHistoryMapper mapper;

    @Override
    @Transactional
    public ConversationHistory save(ConversationHistory conversationHistory) {
        Optional<ConversationHistoryEntity> existingOpt = jpaRepository.findByConversationId(conversationHistory.conversationId());

        if (existingOpt.isPresent()) {
            ConversationHistoryEntity existing = existingOpt.get();
            existing.setEndedAt(conversationHistory.endedAt());
            if (conversationHistory.topic() != null) {
                existing.setTopic(conversationHistory.topic());
            }

            // Add only new participants (by personId)
            Set<String> existingPersonIds = existing.getParticipants().stream()
                    .map(ParticipantHistoryEntity::getPersonId)
                    .collect(Collectors.toSet());
            conversationHistory.participants().stream()
                    .filter(p -> !existingPersonIds.contains(p.personId()))
                    .forEach(p -> existing.addParticipant(ParticipantHistoryEntity.builder()
                            .personId(p.personId())
                            .displayName(p.displayName())
                            .type(ParticipantHistoryEntity.ParticipantType.valueOf(p.participantType().name()))
                            .build()));

            // Add only new events (by offset to avoid duplicates)
            int existingEventCount = existing.getEvents().size();
            int domainEventCount = conversationHistory.events().size();
            if (domainEventCount > existingEventCount) {
                conversationHistory.events().subList(existingEventCount, domainEventCount)
                        .forEach(e -> existing.addEvent(ConversationEventHistoryEntity.builder()
                                .eventType(ConversationEventHistoryEntity.EventType.valueOf(e.eventType().name()))
                                .eventTime(e.occurredAt())
                                .messageText(e.messageText())
                                .senderPersonId(e.senderPersonId())
                                .senderDisplayName(e.senderDisplayName())
                                .build()));
            }

            ConversationHistoryEntity savedEntity = jpaRepository.save(existing);
            return mapper.toDomain(savedEntity);
        }

        ConversationHistoryEntity entity = mapper.toEntity(conversationHistory);
        ConversationHistoryEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConversationHistory> findByConversationId(String conversationId) {
        Optional<ConversationHistoryEntity> entityOpt = jpaRepository.findByConversationId(conversationId);

        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        ConversationHistoryEntity entity = entityOpt.get();

        // Force initialization of lazy collections within transaction
        entity.getParticipants().size();
        entity.getEvents().size();

        return Optional.of(mapper.toDomain(entity));
    }

    @Override
    public boolean existsByConversationId(String conversationId) {
        return jpaRepository.existsByConversationId(conversationId);
    }
}
