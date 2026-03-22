package org.dbs.poc.unblu.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.model.history.ConversationHistoryPage;
import org.dbs.poc.unblu.domain.model.history.ConversationSortDirection;
import org.dbs.poc.unblu.domain.model.history.ConversationSortField;
import org.dbs.poc.unblu.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.infrastructure.persistence.entity.ConversationEventHistoryEntity;
import org.dbs.poc.unblu.infrastructure.persistence.entity.ConversationHistoryEntity;
import org.dbs.poc.unblu.infrastructure.persistence.entity.ParticipantHistoryEntity;
import org.dbs.poc.unblu.infrastructure.persistence.mapper.ConversationHistoryMapper;
import org.dbs.poc.unblu.infrastructure.persistence.repository.ConversationHistoryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    /**
     * Persists a conversation history using upsert semantics.
     * If a record already exists for the conversation ID, only new participants and events are appended.
     *
     * @param conversationHistory the domain object to persist
     * @return the saved domain object (rebuilt from the JPA entity)
     */
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

    /**
     * Loads a conversation history by conversation ID.
     * Lazy collections are force-initialized within the transaction.
     *
     * @param conversationId Unblu conversation identifier
     * @return the domain object, or {@code Optional.empty()} if not found
     */
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

    /**
     * Returns a paginated list of conversation headers, sorted by the given field and direction.
     * Nullable columns ({@code endedAt}, {@code topic}) are always sorted NULLS LAST.
     * Collections (events, participants) are intentionally not loaded for performance.
     *
     * @param page      zero-indexed page number
     * @param size      number of items per page
     * @param sortField the field to sort by
     * @param sortDir   the sort direction
     * @return the page of conversation summaries
     */
    @Override
    @Transactional(readOnly = true)
    public ConversationHistoryPage findPage(int page, int size, ConversationSortField sortField, ConversationSortDirection sortDir) {
        Sort sort = buildSort(sortField, sortDir);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<ConversationHistoryEntity> entityPage = jpaRepository.findAll(pageRequest);
        List<ConversationHistory> items = entityPage.getContent().stream()
                .map(mapper::toDomainSummary)
                .toList();
        return new ConversationHistoryPage(
                items,
                entityPage.getTotalElements(),
                page,
                size,
                entityPage.getTotalPages());
    }

    private Sort buildSort(ConversationSortField sortField, ConversationSortDirection sortDir) {
        String column = switch (sortField) {
            case CREATED_AT -> "createdAt";
            case ENDED_AT   -> "endedAt";
            case TOPIC      -> "topic";
        };
        Sort.Order order = sortDir == ConversationSortDirection.ASC
                ? Sort.Order.asc(column).with(Sort.NullHandling.NULLS_LAST)
                : Sort.Order.desc(column).with(Sort.NullHandling.NULLS_LAST);
        return Sort.by(order);
    }

    /**
     * Checks whether a conversation history record exists for the given ID.
     *
     * @param conversationId Unblu conversation identifier
     * @return {@code true} if a record exists
     */
    @Override
    public boolean existsByConversationId(String conversationId) {
        return jpaRepository.existsByConversationId(conversationId);
    }
}
