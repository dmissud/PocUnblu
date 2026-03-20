package org.dbs.poc.unblu.application.route.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dbs.poc.unblu.application.service.ConversationHistoryService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Camel Processor that dispatches conversation webhook events to the appropriate handler.
 * Extraction of data from the raw payload and delegation to ConversationHistoryService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEventProcessor implements Processor {

    private final ConversationHistoryService conversationHistoryService;

    private static final String EVENT_TYPE_FIELD = "$_type";
    private static final String EVENT_TYPE_FALLBACK_FIELD = "eventType";

    private static final String CONVERSATION_CREATED_EVENT = "ConversationCreatedEvent";
    private static final String CONVERSATION_CREATED_EVENT_TYPE = "conversation.created";
    private static final String CONVERSATION_NEW_MESSAGE_EVENT = "ConversationNewMessageEvent";
    private static final String CONVERSATION_NEW_MESSAGE_EVENT_TYPE = "conversation.new_message";
    private static final String CONVERSATION_ENDED_EVENT = "ConversationEndedEvent";
    private static final String CONVERSATION_ENDED_EVENT_TYPE = "conversation.ended";

    private static final String FIELD_CONVERSATION_ID = "conversationId";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_CONVERSATION_MESSAGE = "conversationMessage";
    private static final String FIELD_SENDER_PERSON = "senderPerson";
    private static final String FIELD_END_REASON = "endReason";
    private static final String FIELD_CONVERSATION = "conversation";
    private static final String FIELD_PARTICIPANTS = "participants";

    @Override
    public void process(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        String eventType = extractEventType(payload);

        log.info("Conversation Event Received - Type: {}", eventType);
        logPayloadFields(payload);

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

    private void handleConversationCreated(Map<String, Object> payload) {
        log.info("NEW CONVERSATION CREATED");

        String conversationId = extractConversationId(payload);
        if (conversationId == null) {
            log.error("Cannot persist conversation: conversationId is null");
            return;
        }

        String topic = null;
        Object conversationObj = payload.get(FIELD_CONVERSATION);
        if (conversationObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conversationData = (Map<String, Object>) conversationObj;
            topic = (String) conversationData.get("topic");

            Object participantsObj = conversationData.get(FIELD_PARTICIPANTS);
            if (participantsObj != null) {
                log.debug("Participants present in payload (parsing not yet implemented)");
            }
        }

        Instant createdAt = extractTimestamp(payload);
        conversationHistoryService.onConversationCreated(conversationId, topic, createdAt);
    }

    private void handleNewMessage(Map<String, Object> payload) {
        log.info("NEW MESSAGE IN CONVERSATION");

        String conversationId = null;
        String messageText = null;
        String senderPersonId = null;
        String senderDisplayName = null;

        Object conversationMessageObj = payload.get(FIELD_CONVERSATION_MESSAGE);
        if (conversationMessageObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = (Map<String, Object>) conversationMessageObj;
            conversationId = (String) messageData.get(FIELD_CONVERSATION_ID);
            messageText = (String) messageData.get("text");

            Object senderPersonObj = messageData.get(FIELD_SENDER_PERSON);
            if (senderPersonObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> senderData = (Map<String, Object>) senderPersonObj;
                senderPersonId = (String) senderData.get("id");
                senderDisplayName = (String) senderData.get("displayName");
            }
        }

        log.info("  Conversation: {}, Sender: {} ({})", conversationId, senderDisplayName, senderPersonId);

        if (conversationId == null) {
            log.error("Cannot persist message: conversationId is null");
            return;
        }

        Instant messageTime = extractTimestamp(payload);
        conversationHistoryService.onNewMessage(conversationId, messageText, senderPersonId, senderDisplayName, messageTime);
    }

    private void handleConversationEnded(Map<String, Object> payload) {
        log.info("CONVERSATION ENDED");

        String conversationId = extractConversationId(payload);
        if (conversationId == null) {
            log.error("Cannot persist conversation end: conversationId is null");
            return;
        }

        Object endReason = payload.get(FIELD_END_REASON);
        if (endReason != null) {
            log.info("  End reason: {}", endReason);
        }

        Instant endedAt = extractTimestamp(payload);
        conversationHistoryService.onConversationEnded(conversationId, endedAt);
    }

    // --- Extraction helpers ---

    private String extractEventType(Map<String, Object> payload) {
        return Optional.ofNullable((String) payload.get(EVENT_TYPE_FIELD))
                .orElseGet(() -> (String) payload.get(EVENT_TYPE_FALLBACK_FIELD));
    }

    private String extractConversationId(Map<String, Object> payload) {
        Object conversationObj = payload.get(FIELD_CONVERSATION);
        if (conversationObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conversationData = (Map<String, Object>) conversationObj;
            String id = (String) conversationData.get("id");
            if (id != null) return id;
        }
        return (String) payload.get(FIELD_CONVERSATION_ID);
    }

    private Instant extractTimestamp(Map<String, Object> payload) {
        Long timestamp = (Long) payload.get(FIELD_TIMESTAMP);
        return timestamp != null ? Instant.ofEpochMilli(timestamp) : Instant.now();
    }

    private boolean isEvent(String eventType, String typeByClass, String typeByField) {
        return typeByClass.equals(eventType) || typeByField.equals(eventType);
    }

    private void logPayloadFields(Map<String, Object> payload) {
        payload.forEach((key, value) -> {
            if (value != null) {
                log.debug("  {}: {}", key, value);
            }
        });
    }
}
