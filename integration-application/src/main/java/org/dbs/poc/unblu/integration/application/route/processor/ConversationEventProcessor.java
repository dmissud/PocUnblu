package org.dbs.poc.unblu.integration.application.route.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dbs.poc.unblu.integration.application.service.ConversationHistoryService;
import org.dbs.poc.unblu.integration.domain.model.webhook.UnbluWebhookPayload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationEventProcessor implements Processor {

    private final ConversationHistoryService historyService;

    @Override
    public void process(Exchange exchange) {
        UnbluWebhookPayload payload = exchange.getIn().getBody(UnbluWebhookPayload.class);
        String eventType = exchange.getIn().getHeader(WebhookEventTypeExtractor.EVENT_TYPE_HEADER, String.class);
        log.info("Processing conversation event: type={}", eventType);

        if (eventType == null) { log.warn("No event type header found"); return; }

        if (eventType.endsWith(".created") || eventType.startsWith("ConversationCreated")) {
            historyService.onConversationCreated(payload);
        } else if (eventType.endsWith(".new_message") || eventType.contains("NewMessage")) {
            historyService.onNewMessage(payload);
        } else if (eventType.endsWith(".ended") || eventType.startsWith("ConversationEnded")) {
            historyService.onConversationEnded(payload);
        } else {
            log.info("Unhandled conversation event type: {} — logged only", eventType);
        }
    }
}
