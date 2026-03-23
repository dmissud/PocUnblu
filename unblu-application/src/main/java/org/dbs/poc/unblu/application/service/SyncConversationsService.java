package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.in.SyncConversationsUseCase;
import org.dbs.poc.unblu.domain.model.ConversationSyncResult;
import org.dbs.poc.unblu.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.domain.model.UnbluMessageData;
import org.dbs.poc.unblu.domain.model.UnbluParticipantData;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.model.history.ParticipantHistory;
import org.dbs.poc.unblu.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.domain.port.out.UnbluPort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation du cas d'utilisation de synchronisation des conversations Unblu.
 *
 * <p>Pour chaque conversation récupérée depuis Unblu :
 * <ul>
 *   <li>Si elle est inconnue en base → création d'une nouvelle entrée {@link ConversationHistory}.</li>
 *   <li>Si elle est déjà connue et vient de se terminer → mise à jour de l'horodatage de fin.</li>
 *   <li>Si elle est déjà connue et non terminée → aucune action (opération idempotente).</li>
 * </ul>
 *
 * <p>Les erreurs par conversation sont isolées : un échec n'interrompt pas le traitement des suivantes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncConversationsService implements SyncConversationsUseCase {

    private final UnbluPort unbluPort;
    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public ConversationSyncResult syncAll() {
        List<UnbluConversationSummary> conversations = unbluPort.listAllConversations();
        if (conversations.isEmpty()) {
            log.warn("Aucune conversation récupérée depuis Unblu — vérifier la connectivité ou les logs du circuit breaker");
        } else {
            log.info("Scan de {} conversation(s) depuis Unblu", conversations.size());
        }

        int newlyPersisted = 0;
        int alreadyExisting = 0;
        List<String> errorIds = new ArrayList<>();

        for (UnbluConversationSummary summary : conversations) {
            try {
                if (conversationHistoryRepository.existsByConversationId(summary.id())) {
                    updateEndedIfNeeded(summary);
                    alreadyExisting++;
                } else {
                    persistNew(summary);
                    newlyPersisted++;
                }
                enrichWithContent(summary.id());
            } catch (Exception e) {
                log.error("Erreur lors de la synchronisation de la conversation {}: {}",
                        summary.id(), e.getMessage(), e);
                errorIds.add(summary.id());
            }
        }

        ConversationSyncResult result = new ConversationSyncResult(
                conversations.size(), newlyPersisted, alreadyExisting, errorIds.size(), errorIds);
        log.info("Synchronisation terminée — total: {}, nouvelles: {}, existantes: {}, erreurs: {}",
                result.totalScanned(), result.newlyPersisted(), result.alreadyExisting(), result.errors());
        return result;
    }

    private void persistNew(UnbluConversationSummary summary) {
        ConversationHistory history = ConversationHistory.create(
                summary.id(), summary.topic(), summary.createdAt());
        if (summary.isEnded()) {
            history.end(summary.endedAt());
        }
        conversationHistoryRepository.save(history);
        log.debug("Conversation {} persistée (nouvelle, état: {})", summary.id(), summary.state());
    }

    private void enrichWithContent(String conversationId) {
        conversationHistoryRepository.findByConversationId(conversationId).ifPresent(history -> {
            boolean enriched = false;

            List<UnbluParticipantData> participants = unbluPort.fetchConversationParticipants(conversationId);
            for (UnbluParticipantData p : participants) {
                ParticipantHistory.ParticipantType type = resolveParticipantType(p.participationType());
                history.registerParticipant(p.personId(), p.displayName(), type);
                enriched = true;
            }

            List<UnbluMessageData> messages = unbluPort.fetchConversationMessages(conversationId);
            for (UnbluMessageData m : messages) {
                history.backfillMessage(m.text(), m.senderPersonId(), null, m.sentAt());
                enriched = true;
            }

            if (enriched) {
                conversationHistoryRepository.save(history);
                log.debug("Conversation {} enrichie : {} participant(s), {} message(s)",
                        conversationId, participants.size(), messages.size());
            }
        });
    }

    private ParticipantHistory.ParticipantType resolveParticipantType(String participationType) {
        if ("ASSIGNED_AGENT".equals(participationType) || "AGENT".equals(participationType)) {
            return ParticipantHistory.ParticipantType.AGENT;
        }
        return ParticipantHistory.ParticipantType.VISITOR;
    }

    private void updateEndedIfNeeded(UnbluConversationSummary summary) {
        if (!summary.isEnded()) {
            return;
        }
        conversationHistoryRepository.findByConversationId(summary.id()).ifPresent(history -> {
            if (!history.isEnded()) {
                history.end(summary.endedAt());
                conversationHistoryRepository.save(history);
                log.debug("Conversation {} mise à jour (terminée à {})", summary.id(), summary.endedAt());
            }
        });
    }
}
