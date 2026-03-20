package org.dbs.poc.unblu.infrastructure.persistence.repository;

import org.dbs.poc.unblu.infrastructure.persistence.entity.ConversationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for ConversationHistoryEntity.
 */
@Repository
public interface ConversationHistoryJpaRepository extends JpaRepository<ConversationHistoryEntity, Long> {

    @Query("SELECT DISTINCT c FROM ConversationHistoryEntity c " +
           "LEFT JOIN FETCH c.participants " +
           "WHERE c.conversationId = :conversationId")
    Optional<ConversationHistoryEntity> findByConversationIdWithParticipants(@Param("conversationId") String conversationId);

    @Query("SELECT DISTINCT c FROM ConversationHistoryEntity c " +
           "LEFT JOIN FETCH c.events " +
           "WHERE c.conversationId = :conversationId")
    Optional<ConversationHistoryEntity> findByConversationIdWithEvents(@Param("conversationId") String conversationId);

    Optional<ConversationHistoryEntity> findByConversationId(String conversationId);

    boolean existsByConversationId(String conversationId);
}
