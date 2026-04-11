package org.dbs.poc.unblu.integration.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistoryPage;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationSortDirection;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationSortField;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.integration.infrastructure.persistence.entity.ConversationEventHistoryEntity;
import org.dbs.poc.unblu.integration.infrastructure.persistence.entity.ConversationHistoryEntity;
import org.dbs.poc.unblu.integration.infrastructure.persistence.entity.ParticipantHistoryEntity;
import org.dbs.poc.unblu.integration.infrastructure.persistence.mapper.ConversationHistoryMapper;
import org.dbs.poc.unblu.integration.infrastructure.persistence.repository.ConversationHistoryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ConversationHistoryRepositoryAdapter implements ConversationHistoryRepository {

    private final ConversationHistoryJpaRepository jpaRepository;
    private final ConversationHistoryMapper mapper;

    @Override
    @Transactional
    public ConversationHistory save(ConversationHistory domain) {
        Optional<ConversationHistoryEntity> existingOpt = jpaRepository.findByConversationId(domain.conversationId());

        if (existingOpt.isPresent()) {
            ConversationHistoryEntity existing = existingOpt.get();
            existing.setEndedAt(domain.endedAt());
            if (domain.topic() != null) {
                existing.setTopic(domain.topic());
            }

            // Append only new participants (by personId)
            Set<String> existingPersonIds = existing.getParticipants().stream()
                    .map(ParticipantHistoryEntity::getPersonId)
                    .collect(Collectors.toSet());
            domain.participants().stream()
                    .filter(p -> !existingPersonIds.contains(p.personId()))
                    .forEach(p -> existing.addParticipant(ParticipantHistoryEntity.builder()
                            .personId(p.personId())
                            .displayName(p.displayName())
                            .type(ParticipantHistoryEntity.ParticipantType.valueOf(p.participantType().name()))
                            .build()));

            // Append only new events (by offset to avoid duplicates)
            int existingEventCount = existing.getEvents().size();
            int domainEventCount = domain.events().size();
            if (domainEventCount > existingEventCount) {
                domain.events().subList(existingEventCount, domainEventCount)
                        .forEach(e -> existing.addEvent(ConversationEventHistoryEntity.builder()
                                .eventType(ConversationEventHistoryEntity.EventType.valueOf(e.eventType().name()))
                                .eventTime(e.occurredAt())
                                .messageId(e.messageId())
                                .messageText(e.messageText())
                                .senderPersonId(e.senderPersonId())
                                .senderDisplayName(e.senderDisplayName())
                                .build()));
            }

            return mapper.toDomain(jpaRepository.save(existing));
        }

        return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConversationHistory> findByConversationId(String conversationId) {
        Optional<ConversationHistoryEntity> entityOpt = jpaRepository.findByConversationId(conversationId);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }
        ConversationHistoryEntity entity = entityOpt.get();
        // Force initialization of lazy collections within the transaction
        entity.getParticipants().size();
        entity.getEvents().size();
        return Optional.of(mapper.toDomain(entity));
    }

    @Override
    public ConversationHistoryPage findPage(int page, int size, ConversationSortField sortField, ConversationSortDirection sortDir) {
        boolean asc = sortDir == ConversationSortDirection.ASC;
        Pageable unsorted = PageRequest.of(page, size);
        Page<ConversationHistoryEntity> entityPage = switch (sortField) {
            case CREATED_AT -> {
                Sort sort = asc ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
                yield jpaRepository.findAll(PageRequest.of(page, size, sort));
            }
            case ENDED_AT -> asc ? jpaRepository.findAllOrderByEndedAtAsc(unsorted) : jpaRepository.findAllOrderByEndedAtDesc(unsorted);
            case TOPIC    -> asc ? jpaRepository.findAllOrderByTopicAsc(unsorted) : jpaRepository.findAllOrderByTopicDesc(unsorted);
        };
        List<ConversationHistory> items = entityPage.getContent().stream().map(mapper::toDomainSummary).toList();
        return new ConversationHistoryPage(items, entityPage.getTotalElements(), page, size, entityPage.getTotalPages());
    }

    @Override
    public boolean existsByConversationId(String conversationId) {
        return jpaRepository.existsByConversationId(conversationId);
    }

}
