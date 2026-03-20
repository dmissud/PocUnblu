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
        Optional<ConversationHistoryEntity> existingOpt = jpaRepository.findByConversationId(conversationHistory.getConversationId());

        if (existingOpt.isPresent()) {
            ConversationHistoryEntity existing = existingOpt.get();
            existing.setEndedAt(conversationHistory.getEndedAt());
            if (conversationHistory.getTopic() != null) {
                existing.setTopic(conversationHistory.getTopic());
            }

            // Add only new participants (by personId)
            Set<String> existingPersonIds = existing.getParticipants().stream()
                    .map(ParticipantHistoryEntity::getPersonId)
                    .collect(Collectors.toSet());
            conversationHistory.getParticipants().stream()
                    .filter(p -> !existingPersonIds.contains(p.getPersonId()))
                    .forEach(p -> existing.addParticipant(ParticipantHistoryEntity.builder()
                            .personId(p.getPersonId())
                            .displayName(p.getDisplayName())
                            .type(ParticipantHistoryEntity.ParticipantType.valueOf(p.getType().name()))
                            .build()));

            // Add only new events (by eventTime + eventType to avoid duplicates)
            int existingEventCount = existing.getEvents().size();
            int domainEventCount = conversationHistory.getEvents().size();
            if (domainEventCount > existingEventCount) {
                conversationHistory.getEvents().subList(existingEventCount, domainEventCount)
                        .forEach(e -> existing.addEvent(ConversationEventHistoryEntity.builder()
                                .eventType(ConversationEventHistoryEntity.EventType.valueOf(e.getEventType().name()))
                                .eventTime(e.getEventTime())
                                .messageText(e.getMessageText())
                                .senderPersonId(e.getSenderPersonId())
                                .senderDisplayName(e.getSenderDisplayName())
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

        // Map to domain with both collections loaded
        return Optional.of(mapper.toDomain(entity));
    }

    @Override
    public boolean existsByConversationId(String conversationId) {
        return jpaRepository.existsByConversationId(conversationId);
    }
}
