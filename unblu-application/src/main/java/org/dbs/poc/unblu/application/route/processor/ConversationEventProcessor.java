package org.dbs.poc.unblu.application.route.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dbs.poc.unblu.application.route.webhook.UnbluWebhookPayload;
import org.dbs.poc.unblu.application.service.ConversationHistoryService;
import org.springframework.stereotype.Component;

/**
 * Camel Processor that dispatches conversation webhook events to {@link ConversationHistoryService}.
 * Responsibility: read the event type, delegate — nothing more.
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

    /**
     * Reads the event type from the payload and delegates to {@link ConversationHistoryService}.
     * Handles: {@code ConversationCreatedEvent}, {@code ConversationNewMessageEvent}, {@code ConversationEndedEvent}.
     */
    @Override
    public void process(Exchange exchange) {
        UnbluWebhookPayload payload = exchange.getIn().getBody(UnbluWebhookPayload.class);
        String eventType = payload.type() != null ? payload.type() : payload.eventType();

        log.info("Conversation Event - Type: {}", eventType);

        if (matches(eventType, CONVERSATION_CREATED_EVENT, CONVERSATION_CREATED_EVENT_TYPE)) {
            conversationHistoryService.onConversationCreated(payload);
        } else if (matches(eventType, CONVERSATION_NEW_MESSAGE_EVENT, CONVERSATION_NEW_MESSAGE_EVENT_TYPE)) {
            conversationHistoryService.onNewMessage(payload);
        } else if (matches(eventType, CONVERSATION_ENDED_EVENT, CONVERSATION_ENDED_EVENT_TYPE)) {
            conversationHistoryService.onConversationEnded(payload);
        } else {
            log.debug("Unhandled conversation event type: {}", eventType);
        }
    }

    /**
     * Returns {@code true} if {@code eventType} matches either the class name or the field notation.
     */
    private boolean matches(String eventType, String byClass, String byField) {
        return byClass.equals(eventType) || byField.equals(eventType);
    }
}
