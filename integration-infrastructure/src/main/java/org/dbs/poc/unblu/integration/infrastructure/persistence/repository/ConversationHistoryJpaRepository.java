package org.dbs.poc.unblu.integration.infrastructure.persistence.repository;

import org.dbs.poc.unblu.integration.infrastructure.persistence.entity.ConversationHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationHistoryJpaRepository extends JpaRepository<ConversationHistoryEntity, Long> {

    Optional<ConversationHistoryEntity> findByConversationId(String conversationId);

    boolean existsByConversationId(String conversationId);

    @Query("SELECT c FROM ConversationHistoryEntity c ORDER BY c.endedAt ASC NULLS LAST")
    Page<ConversationHistoryEntity> findAllOrderByEndedAtAsc(Pageable pageable);

    @Query("SELECT c FROM ConversationHistoryEntity c ORDER BY c.endedAt DESC NULLS LAST")
    Page<ConversationHistoryEntity> findAllOrderByEndedAtDesc(Pageable pageable);

    @Query("SELECT c FROM ConversationHistoryEntity c ORDER BY c.topic ASC NULLS LAST")
    Page<ConversationHistoryEntity> findAllOrderByTopicAsc(Pageable pageable);

    @Query("SELECT c FROM ConversationHistoryEntity c ORDER BY c.topic DESC NULLS LAST")
    Page<ConversationHistoryEntity> findAllOrderByTopicDesc(Pageable pageable);
}
