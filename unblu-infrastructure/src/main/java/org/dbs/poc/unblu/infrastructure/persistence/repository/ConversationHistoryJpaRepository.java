package org.dbs.poc.unblu.infrastructure.persistence.repository;

import org.dbs.poc.unblu.infrastructure.persistence.entity.ConversationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour {@link ConversationHistoryEntity}.
 * Fournit des requêtes optimisées avec chargement eager des collections par JPQL.
 */
@Repository
public interface ConversationHistoryJpaRepository extends JpaRepository<ConversationHistoryEntity, Long> {

    /**
     * Recherche une conversation avec ses participants pré-chargés (évite le problème N+1).
     *
     * @param conversationId l'identifiant Unblu de la conversation
     * @return la conversation avec ses participants, ou {@link Optional#empty()} si non trouvée
     */
    @Query("SELECT DISTINCT c FROM ConversationHistoryEntity c " +
           "LEFT JOIN FETCH c.participants " +
           "WHERE c.conversationId = :conversationId")
    Optional<ConversationHistoryEntity> findByConversationIdWithParticipants(@Param("conversationId") String conversationId);

    /**
     * Recherche une conversation avec ses événements pré-chargés (évite le problème N+1).
     *
     * @param conversationId l'identifiant Unblu de la conversation
     * @return la conversation avec ses événements, ou {@link Optional#empty()} si non trouvée
     */
    @Query("SELECT DISTINCT c FROM ConversationHistoryEntity c " +
           "LEFT JOIN FETCH c.events " +
           "WHERE c.conversationId = :conversationId")
    Optional<ConversationHistoryEntity> findByConversationIdWithEvents(@Param("conversationId") String conversationId);

    /**
     * Recherche une conversation par son identifiant Unblu.
     *
     * @param conversationId l'identifiant Unblu de la conversation
     * @return la conversation trouvée, ou {@link Optional#empty()} si inexistante
     */
    Optional<ConversationHistoryEntity> findByConversationId(String conversationId);

    /**
     * Vérifie si une conversation existe déjà pour l'identifiant donné.
     *
     * @param conversationId l'identifiant Unblu de la conversation
     * @return {@code true} si la conversation existe en base, {@code false} sinon
     */
    boolean existsByConversationId(String conversationId);
}
