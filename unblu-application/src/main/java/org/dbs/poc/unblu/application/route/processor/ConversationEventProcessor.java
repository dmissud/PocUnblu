package org.dbs.poc.unblu.application.route.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dbs.poc.unblu.domain.model.history.ConversationEventHistory;
import org.dbs.poc.unblu.domain.model.history.ConversationHistory;
import org.dbs.poc.unblu.domain.model.history.ParticipantHistory;
import org.dbs.poc.unblu.domain.port.out.ConversationHistoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Processes conversation-related webhook events and persists them to database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEventProcessor implements Processor {

    private final ConversationHistoryRepository conversationHistoryRepository;

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
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_CONVERSATION_MESSAGE = "conversationMessage";
    private static final String FIELD_SENDER_PERSON = "senderPerson";
    private static final String FIELD_END_REASON = "endReason";
    private static final String FIELD_CONVERSATION = "conversation";
    private static final String FIELD_PARTICIPANTS = "participants";

    @Override
    public void process(Exchange exchange) {
        Map<String, Object> payload = exchange.getIn().getBody(Map.class);
        String eventType = extractEventType(payload);

        log.info("🔔 Conversation Event Received!");
        log.info("   Type: {}", eventType);
        log.info("   Conversation ID: {}", payload.get(FIELD_CONVERSATION_ID));
        log.info("   Full Details:");

        logPayloadFields(payload);

        if (isConversationCreatedEvent(eventType)) {
            handleConversationCreated(payload);
        } else if (isConversationNewMessageEvent(eventType)) {
            handleNewMessage(payload);
        } else if (isConversationEndedEvent(eventType)) {
            handleConversationEnded(payload);
        }
    }

    private String extractEventType(Map<String, Object> payload) {
        return Optional.ofNullable((String) payload.get(EVENT_TYPE_FIELD))
                .orElseGet(() -> (String) payload.get(EVENT_TYPE_FALLBACK_FIELD));
    }

    private void logPayloadFields(Map<String, Object> payload) {
        payload.forEach((key, value) -> {
            if (value != null) {
                log.info("      • {}: {}", key, value);
            }
        });
    }

    private boolean isConversationCreatedEvent(String eventType) {
        return CONVERSATION_CREATED_EVENT.equals(eventType)
                || CONVERSATION_CREATED_EVENT_TYPE.equals(eventType);
    }

    /**
     * Extract conversation ID from webhook payload.
     * Tries nested conversation object first, then falls back to root level.
     */
    private String extractConversationId(Map<String, Object> payload) {
        String conversationId = null;

        // Try to get from nested conversation object
        Object conversationObj = payload.get(FIELD_CONVERSATION);
        if (conversationObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conversationData = (Map<String, Object>) conversationObj;
            conversationId = (String) conversationData.get("id");
        }

        // Fallback to root level if present
        if (conversationId == null) {
            conversationId = (String) payload.get(FIELD_CONVERSATION_ID);
        }

        return conversationId;
    }

    @Transactional
    private void handleConversationCreated(Map<String, Object> payload) {
        log.info("");
        log.info("🎉 NEW CONVERSATION CREATED!");
        log.info("   This is the event you configured in the webhook!");
        log.info("");

        // Extract conversation ID from nested conversation object
        String conversationId = extractConversationId(payload);
        Long timestamp = (Long) payload.get(FIELD_TIMESTAMP);
        Instant createdAt = timestamp != null ? Instant.ofEpochMilli(timestamp) : Instant.now();

        log.info("🔍 Persisting conversation: {}", conversationId);

        if (conversationId == null) {
            log.error("❌ Cannot persist conversation: conversationId is null!");
            return;
        }

        // Create conversation history
        ConversationHistory history = ConversationHistory.builder()
                .conversationId(conversationId)
                .createdAt(createdAt)
                .build();

        // Extract participants from conversation object if present
        Object conversationObj = payload.get(FIELD_CONVERSATION);
        if (conversationObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conversationData = (Map<String, Object>) conversationObj;
            Object participantsObj = conversationData.get(FIELD_PARTICIPANTS);

            if (participantsObj != null) {
                log.info("   Extracting participants...");
                // TODO: Parse participants properly when structure is known
            }
        }

        // Add creation event
        ConversationEventHistory createdEvent = ConversationEventHistory.builder()
                .eventType(ConversationEventHistory.EventType.CREATED)
                .eventTime(createdAt)
                .build();
        history.addEvent(createdEvent);

        // Save to database
        try {
            ConversationHistory saved = conversationHistoryRepository.save(history);
            log.info("✅ Conversation history saved to database with ID: {}", saved.getConversationId());
        } catch (Exception e) {
            log.error("❌ Failed to save conversation history", e);
            throw e;
        }
    }

    private boolean isConversationNewMessageEvent(String eventType) {
        return CONVERSATION_NEW_MESSAGE_EVENT.equals(eventType)
                || CONVERSATION_NEW_MESSAGE_EVENT_TYPE.equals(eventType);
    }

    private boolean isConversationEndedEvent(String eventType) {
        return CONVERSATION_ENDED_EVENT.equals(eventType)
                || CONVERSATION_ENDED_EVENT_TYPE.equals(eventType);
    }

    @Transactional
    private void handleNewMessage(Map<String, Object> payload) {
        log.info("");
        log.info("💬 NEW MESSAGE IN CONVERSATION!");

        // Extract conversation ID from conversationMessage object
        String conversationId = null;
        Object conversationMessageObj = payload.get(FIELD_CONVERSATION_MESSAGE);
        if (conversationMessageObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conversationMessageData = (Map<String, Object>) conversationMessageObj;
            conversationId = (String) conversationMessageData.get(FIELD_CONVERSATION_ID);
        }

        Long timestamp = (Long) payload.get(FIELD_TIMESTAMP);
        Instant messageTime = timestamp != null ? Instant.ofEpochMilli(timestamp) : Instant.now();

        // Message and sender are also in conversationMessage
        String messageText = null;
        String senderPersonId = null;
        String senderDisplayName = null;

        if (conversationMessageObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> conversationMessageData = (Map<String, Object>) conversationMessageObj;

            // Extract text
            messageText = (String) conversationMessageData.get("text");

            // Extract sender person info
            Object senderPersonObj = conversationMessageData.get(FIELD_SENDER_PERSON);
            if (senderPersonObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> senderData = (Map<String, Object>) senderPersonObj;
                senderPersonId = (String) senderData.get("id");
                senderDisplayName = (String) senderData.get("displayName");
            }
        }

        log.info("   Conversation ID: {}", conversationId);
        log.info("   Message Text: {}", messageText);
        log.info("   Sender: {} ({})", senderDisplayName, senderPersonId);

        if (conversationId == null) {
            log.error("❌ Cannot persist message: conversationId is null!");
            return;
        }

        // Find existing conversation
        Optional<ConversationHistory> historyOpt = conversationHistoryRepository.findByConversationId(conversationId);

        if (historyOpt.isPresent()) {
            ConversationHistory history = historyOpt.get();
            
            // Add message event
            ConversationEventHistory messageEvent = ConversationEventHistory.builder()
                    .eventType(ConversationEventHistory.EventType.MESSAGE)
                    .eventTime(messageTime)
                    .messageText(messageText)
                    .senderPersonId(senderPersonId)
                    .senderDisplayName(senderDisplayName)
                    .build();
            history.addEvent(messageEvent);
            
            // Update participant if new
            final String finalSenderPersonId = senderPersonId;
            if (senderPersonId != null && senderDisplayName != null) {
                boolean participantExists = history.getParticipants().stream()
                        .anyMatch(p -> p.getPersonId().equals(finalSenderPersonId));
                
                if (!participantExists) {
                    ParticipantHistory participant = ParticipantHistory.builder()
                            .personId(senderPersonId)
                            .displayName(senderDisplayName)
                            .type(ParticipantHistory.ParticipantType.VISITOR) // Default, will be refined
                            .build();
                    history.addParticipant(participant);
                    log.info("   New participant added: {}", senderDisplayName);
                }
            }
            
            try {
                conversationHistoryRepository.save(history);
                log.info("✅ Message saved to database!");
            } catch (Exception e) {
                log.error("❌ Failed to save message", e);
                throw e;
            }
        } else {
            log.warn("⚠️  Conversation {} not found in database!", conversationId);
        }

        log.info("");
    }

    @Transactional
    private void handleConversationEnded(Map<String, Object> payload) {
        log.info("");
        log.info("🏁 CONVERSATION ENDED!");

        // Extract conversation ID from nested conversation object
        String conversationId = extractConversationId(payload);
        Long timestamp = (Long) payload.get(FIELD_TIMESTAMP);
        Instant endedAt = timestamp != null ? Instant.ofEpochMilli(timestamp) : Instant.now();

        Object endReason = payload.get(FIELD_END_REASON);

        if (endReason != null) {
            log.info("   End Reason: {}", endReason);
        }

        // Find existing conversation and update end time
        Optional<ConversationHistory> historyOpt = conversationHistoryRepository.findByConversationId(conversationId);
        
        if (historyOpt.isPresent()) {
            ConversationHistory history = historyOpt.get();
            history.setEndedAt(endedAt);
            
            // Add ended event
            ConversationEventHistory endedEvent = ConversationEventHistory.builder()
                    .eventType(ConversationEventHistory.EventType.ENDED)
                    .eventTime(endedAt)
                    .build();
            history.addEvent(endedEvent);
            
            try {
                conversationHistoryRepository.save(history);
                log.info("✅ Conversation end saved to database!");
            } catch (Exception e) {
                log.error("❌ Failed to save conversation end", e);
                throw e;
            }
        } else {
            log.warn("⚠️  Conversation {} not found in database!", conversationId);
        }

        log.info("");
    }
}
