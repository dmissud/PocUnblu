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

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationHistoryService {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Transactional
    public void onConversationCreated(UnbluWebhookPayload payload) {
        String conversationId = payload.extractConversationId();
        if (conversationId == null) {
            log.error("Cannot persist conversation: conversationId is null");
            return;
        }

        String topic = (payload.conversation() != null) ? payload.conversation().topic() : null;
        Instant createdAt = payload.extractTimestamp();

        log.info("Persisting new conversation: {} (topic: {})", conversationId, topic);
        ConversationHistory history = ConversationHistory.create(conversationId, topic, createdAt);
        conversationHistoryRepository.save(history);
        log.info("Conversation history saved: {}", conversationId);
    }

    @Transactional
    public void onNewMessage(UnbluWebhookPayload payload) {
        ConversationMessageData message = payload.conversationMessage();
        if (message == null) {
            log.error("Cannot persist message: conversationMessage is null");
            return;
        }

        String conversationId = (message.conversationId() != null) ? message.conversationId() : payload.extractConversationId();
        if (conversationId == null) {
            log.error("Cannot persist message: conversationId is null");
            return;
        }

        String senderPersonId = (message.senderPerson() != null) ? message.senderPerson().id() : null;
        String senderDisplayName = (message.senderPerson() != null) ? message.senderPerson().displayName() : null;
        Instant messageTime = payload.extractTimestamp();

        log.info("  Conversation: {}, Sender: {} ({})", conversationId, senderDisplayName, senderPersonId);

        conversationHistoryRepository.findByConversationId(conversationId).ifPresentOrElse(
                history -> {
                    history.recordMessage(message.text(), senderPersonId, senderDisplayName, messageTime);
                    if (senderPersonId != null && senderDisplayName != null) {
                        history.registerParticipant(senderPersonId, senderDisplayName, ParticipantHistory.ParticipantType.VISITOR);
                    }
                    conversationHistoryRepository.save(history);
                },
                () -> log.warn("Conversation {} not found", conversationId)
        );
    }

    @Transactional
    public void onConversationEnded(UnbluWebhookPayload payload) {
        String conversationId = payload.extractConversationId();
        if (conversationId == null) {
            log.error("Cannot persist end: conversationId is null");
            return;
        }

        conversationHistoryRepository.findByConversationId(conversationId).ifPresentOrElse(
                history -> {
                    history.end(payload.extractTimestamp());
                    conversationHistoryRepository.save(history);
                    log.info("Conversation end saved: {}", conversationId);
                },
                () -> log.warn("Conversation {} not found for end event", conversationId)
        );
    }
}
