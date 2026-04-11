package org.dbs.poc.unblu.integration.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.integration.domain.model.history.ParticipantHistory;
import org.dbs.poc.unblu.integration.domain.model.webhook.ConversationMessageData;
import org.dbs.poc.unblu.integration.domain.model.webhook.UnbluWebhookPayload;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationHistoryService {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Transactional
    public void onConversationCreated(UnbluWebhookPayload payload) {
        String conversationId = extractConversationId(payload);
        if (conversationId == null) { log.error("Cannot persist conversation: conversationId is null"); return; }
        String topic = payload.conversation() != null ? payload.conversation().topic() : null;
        Instant createdAt = extractTimestamp(payload);
        log.info("Persisting new conversation: {} (topic: {})", conversationId, topic);
        ConversationHistory history = ConversationHistory.create(conversationId, topic, createdAt);
        conversationHistoryRepository.save(history);
        log.info("Conversation history saved: {}", conversationId);
    }

    @Transactional
    public void onNewMessage(UnbluWebhookPayload payload) {
        ConversationMessageData message = payload.conversationMessage();
        if (message == null) { log.error("Cannot persist message: conversationMessage is null"); return; }
        String conversationId = message.conversationId();
        if (conversationId == null) { log.error("Cannot persist message: conversationId is null"); return; }
        String senderPersonId = message.senderPerson() != null ? message.senderPerson().id() : null;
        String senderDisplayName = message.senderPerson() != null ? message.senderPerson().displayName() : null;
        Instant messageTime = extractTimestamp(payload);
        log.info("  Conversation: {}, Sender: {} ({})", conversationId, senderDisplayName, senderPersonId);
        Optional<ConversationHistory> historyOpt = conversationHistoryRepository.findByConversationId(conversationId);
        if (historyOpt.isEmpty()) { log.warn("Conversation {} not found", conversationId); return; }
        ConversationHistory history = historyOpt.get();
        history.recordMessage(message.text(), senderPersonId, senderDisplayName, messageTime);
        if (senderPersonId != null && senderDisplayName != null) {
            history.registerParticipant(senderPersonId, senderDisplayName, ParticipantHistory.ParticipantType.VISITOR);
        }
        conversationHistoryRepository.save(history);
    }

    @Transactional
    public void onConversationEnded(UnbluWebhookPayload payload) {
        String conversationId = extractConversationId(payload);
        if (conversationId == null) { log.error("Cannot persist end: conversationId is null"); return; }
        conversationHistoryRepository.findByConversationId(conversationId).ifPresentOrElse(
                history -> { history.end(extractTimestamp(payload)); conversationHistoryRepository.save(history);
                    log.info("Conversation end saved: {}", conversationId); },
                () -> log.warn("Conversation {} not found for end event", conversationId));
    }

    private String extractConversationId(UnbluWebhookPayload payload) {
        if (payload.conversation() != null && payload.conversation().id() != null) return payload.conversation().id();
        return payload.conversationId();
    }

    private Instant extractTimestamp(UnbluWebhookPayload payload) {
        return payload.timestamp() != null ? Instant.ofEpochMilli(payload.timestamp()) : Instant.now();
    }
}
