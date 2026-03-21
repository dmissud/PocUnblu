package org.dbs.poc.unblu.application.route.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dbs.poc.unblu.application.route.webhook.ConversationMessageData;
import org.dbs.poc.unblu.application.route.webhook.UnbluWebhookPayload;
import org.dbs.poc.unblu.application.service.ConversationHistoryService;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Camel Processor that dispatches conversation webhook events to the appropriate handler.
 * Receives a typed {@link UnbluWebhookPayload} — no Map parsing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEventProcessor implements Processor {

    private final ConversationHistoryService conversationHistoryService;

    private static final String CONVERSATION_CREATED_EVENT = "ConversationCreatedEvent";
    private static final String CONVERSATION_CREATED_EVENT_TYPE = "conversation.created";
    private static final String CONVERSATION_NEW_MESSAGE_EVENT = "ConversationNewMessageEvent";
    private static final String CONVERSATION_NEW_MESSAGE_EVENT_TYPE = "conversation.new_message";
    private static final String CONVERSATION_ENDED_EVENT = "ConversationEndedEvent";
    private static final String CONVERSATION_ENDED_EVENT_TYPE = "conversation.ended";

    @Override
    public void process(Exchange exchange) {
        UnbluWebhookPayload payload = exchange.getIn().getBody(UnbluWebhookPayload.class);
        String eventType = payload.type() != null ? payload.type() : payload.eventType();

        log.info("Conversation Event Received - Type: {}", eventType);

        if (isEvent(eventType, CONVERSATION_CREATED_EVENT, CONVERSATION_CREATED_EVENT_TYPE)) {
            handleConversationCreated(payload);
        } else if (isEvent(eventType, CONVERSATION_NEW_MESSAGE_EVENT, CONVERSATION_NEW_MESSAGE_EVENT_TYPE)) {
            handleNewMessage(payload);
        } else if (isEvent(eventType, CONVERSATION_ENDED_EVENT, CONVERSATION_ENDED_EVENT_TYPE)) {
            handleConversationEnded(payload);
        } else {
            log.debug("Unhandled conversation event type: {}", eventType);
        }
    }

    private void handleConversationCreated(UnbluWebhookPayload payload) {
        log.info("NEW CONVERSATION CREATED");

        String conversationId = extractConversationId(payload);
        if (conversationId == null) {
            log.error("Cannot persist conversation: conversationId is null");
            return;
        }

        String topic = payload.conversation() != null ? payload.conversation().topic() : null;
        Instant createdAt = extractTimestamp(payload);
        conversationHistoryService.onConversationCreated(conversationId, topic, createdAt);
    }

    private void handleNewMessage(UnbluWebhookPayload payload) {
        log.info("NEW MESSAGE IN CONVERSATION");

        ConversationMessageData message = payload.conversationMessage();
        if (message == null) {
            log.error("Cannot persist message: conversationMessage is null");
            return;
        }

        String conversationId = message.conversationId();
        String messageText = message.text();
        String senderPersonId = message.senderPerson() != null ? message.senderPerson().id() : null;
        String senderDisplayName = message.senderPerson() != null ? message.senderPerson().displayName() : null;

        log.info("  Conversation: {}, Sender: {} ({})", conversationId, senderDisplayName, senderPersonId);

        if (conversationId == null) {
            log.error("Cannot persist message: conversationId is null");
            return;
        }

        Instant messageTime = extractTimestamp(payload);
        conversationHistoryService.onNewMessage(conversationId, messageText, senderPersonId, senderDisplayName, messageTime);
    }

    private void handleConversationEnded(UnbluWebhookPayload payload) {
        log.info("CONVERSATION ENDED");

        String conversationId = extractConversationId(payload);
        if (conversationId == null) {
            log.error("Cannot persist conversation end: conversationId is null");
            return;
        }

        if (payload.endReason() != null) {
            log.info("  End reason: {}", payload.endReason());
        }

        Instant endedAt = extractTimestamp(payload);
        conversationHistoryService.onConversationEnded(conversationId, endedAt);
    }

    private String extractConversationId(UnbluWebhookPayload payload) {
        if (payload.conversation() != null && payload.conversation().id() != null) {
            return payload.conversation().id();
        }
        return payload.conversationId();
    }

    private Instant extractTimestamp(UnbluWebhookPayload payload) {
        return payload.timestamp() != null ? Instant.ofEpochMilli(payload.timestamp()) : Instant.now();
    }

    private boolean isEvent(String eventType, String typeByClass, String typeByField) {
        return typeByClass.equals(eventType) || typeByField.equals(eventType);
    }
}
