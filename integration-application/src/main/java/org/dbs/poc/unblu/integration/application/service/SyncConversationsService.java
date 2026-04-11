package org.dbs.poc.unblu.integration.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.application.port.in.SyncConversationsUseCase;
import org.dbs.poc.unblu.integration.domain.model.ConversationSyncResult;
import org.dbs.poc.unblu.integration.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.dbs.poc.unblu.integration.domain.port.out.IntegrationUnbluPort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncConversationsService implements SyncConversationsUseCase {

    private final IntegrationUnbluPort unbluPort;
    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public ConversationSyncResult syncAll() {
        List<UnbluConversationSummary> conversations = unbluPort.listAllConversations();
        log.info("Scan de {} conversation(s) depuis Unblu", conversations.size());
        int newlyPersisted = 0, alreadyExisting = 0;
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
            } catch (Exception e) {
                log.error("Error syncing conversation {}: {}", summary.id(), e.getMessage(), e);
                errorIds.add(summary.id());
            }
        }
        ConversationSyncResult result = new ConversationSyncResult(
                conversations.size(), newlyPersisted, alreadyExisting, errorIds.size(), errorIds);
        log.info("Sync done — total:{}, new:{}, existing:{}, errors:{}", result.totalScanned(), result.newlyPersisted(), result.alreadyExisting(), result.errors());
        return result;
    }

    private void persistNew(UnbluConversationSummary summary) {
        ConversationHistory h = ConversationHistory.create(summary.id(), summary.topic(), summary.createdAt());
        if (summary.isEnded()) h.end(summary.endedAt());
        conversationHistoryRepository.save(h);
    }

    private void updateEndedIfNeeded(UnbluConversationSummary summary) {
        if (!summary.isEnded()) return;
        conversationHistoryRepository.findByConversationId(summary.id()).ifPresent(h -> {
            if (!h.isEnded()) { h.end(summary.endedAt()); conversationHistoryRepository.save(h); }
        });
    }
}
