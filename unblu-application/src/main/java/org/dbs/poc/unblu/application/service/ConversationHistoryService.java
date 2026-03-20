package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.history.ConversationEventHistory;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.model.history.ParticipantHistory;
import org.dbs.poc.unblu.domain.port.out.ConversationHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Application service responsible for persisting conversation history events.
 * Each method is transactional and called from the webhook event processor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationHistoryService {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Transactional
    public void onConversationCreated(String conversationId, String topic, Instant createdAt) {
        log.info("Persisting new conversation: {} (topic: {})", conversationId, topic);

        ConversationHistory history = ConversationHistory.builder()
                .conversationId(conversationId)
                .topic(topic)
                .createdAt(createdAt)
                .build();

        history.addEvent(ConversationEventHistory.builder()
                .eventType(ConversationEventHistory.EventType.CREATED)
                .eventTime(createdAt)
                .build());

        ConversationHistory saved = conversationHistoryRepository.save(history);
        log.info("Conversation history saved: {}", saved.getConversationId());
    }

    @Transactional
    public void onNewMessage(String conversationId, String messageText,
                             String senderPersonId, String senderDisplayName, Instant messageTime) {
        Optional<ConversationHistory> historyOpt = conversationHistoryRepository.findByConversationId(conversationId);

        if (historyOpt.isEmpty()) {
            log.warn("Conversation {} not found in database, cannot persist message", conversationId);
            return;
        }

        ConversationHistory history = historyOpt.get();

        history.addEvent(ConversationEventHistory.builder()
                .eventType(ConversationEventHistory.EventType.MESSAGE)
                .eventTime(messageTime)
                .messageText(messageText)
                .senderPersonId(senderPersonId)
                .senderDisplayName(senderDisplayName)
                .build());

        if (senderPersonId != null && senderDisplayName != null) {
            boolean participantExists = history.getParticipants().stream()
                    .anyMatch(p -> p.getPersonId().equals(senderPersonId));
            if (!participantExists) {
                history.addParticipant(ParticipantHistory.builder()
                        .personId(senderPersonId)
                        .displayName(senderDisplayName)
                        .type(ParticipantHistory.ParticipantType.VISITOR)
                        .build());
                log.info("New participant added: {}", senderDisplayName);
            }
        }

        conversationHistoryRepository.save(history);
        log.info("Message saved for conversation: {}", conversationId);
    }

    @Transactional
    public void onConversationEnded(String conversationId, Instant endedAt) {
        Optional<ConversationHistory> historyOpt = conversationHistoryRepository.findByConversationId(conversationId);

        if (historyOpt.isEmpty()) {
            log.warn("Conversation {} not found in database, cannot persist end", conversationId);
            return;
        }

        ConversationHistory history = historyOpt.get();
        history.setEndedAt(endedAt);
        history.addEvent(ConversationEventHistory.builder()
                .eventType(ConversationEventHistory.EventType.ENDED)
                .eventTime(endedAt)
                .build());

        conversationHistoryRepository.save(history);
        log.info("Conversation end saved: {}", conversationId);
    }
}
