package org.dbs.poc.unblu.integration.application.route;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.integration.application.route.processor.ConversationEventProcessor;
import org.dbs.poc.unblu.integration.application.route.processor.PersonEventProcessor;
import org.dbs.poc.unblu.integration.application.route.processor.UnknownEventProcessor;
import org.dbs.poc.unblu.integration.application.route.processor.WebhookEventTypeExtractor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebhookEventRoute extends RouteBuilder {

    private static final String DIRECT_EVENT_PROCESSOR    = "direct:webhook-event-processor";
    private static final String DIRECT_CONVERSATION       = "direct:webhook-handle-conversation";
    private static final String DIRECT_PERSON             = "direct:webhook-handle-person";
    private static final String DIRECT_UNKNOWN            = "direct:webhook-handle-unknown";
    private static final String CONV_PREFIX = "conversation.";
    private static final String CONV_CLASS  = "Conversation";
    private static final String PERSON_PREFIX = "person.";
    private static final String PERSON_CLASS  = "Person";

    private final WebhookEventTypeExtractor extractor;
    private final ConversationEventProcessor conversationProcessor;
    private final PersonEventProcessor personProcessor;
    private final UnknownEventProcessor unknownProcessor;

    public WebhookEventRoute(WebhookEventTypeExtractor extractor,
                             ConversationEventProcessor conversationProcessor,
                             PersonEventProcessor personProcessor,
                             UnknownEventProcessor unknownProcessor) {
        this.extractor = extractor;
        this.conversationProcessor = conversationProcessor;
        this.personProcessor = personProcessor;
        this.unknownProcessor = unknownProcessor;
    }

    @Override
    public void configure() {
        onException(DataIntegrityViolationException.class)
                .handled(true)
                .log(LoggingLevel.WARN, "webhook-event-processor", "Duplicate event ignored: ${exception.message}");

        onException(IllegalArgumentException.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "webhook-event-processor", "Invalid event data: ${exception.message}");

        onException(Exception.class)
                .maximumRedeliveries(3).redeliveryDelay(2000).backOffMultiplier(2).useExponentialBackOff()
                .handled(true)
                .log(LoggingLevel.ERROR, "webhook-dead-letter", "Event processing failed: ${exception.message}");

        from(DIRECT_EVENT_PROCESSOR).routeId("webhook-event-processor")
                .log("Processing webhook event")
                .process(extractor)
                .choice()
                    .when(header(WebhookEventTypeExtractor.EVENT_TYPE_HEADER).startsWith(CONV_PREFIX)).to(DIRECT_CONVERSATION)
                    .when(header(WebhookEventTypeExtractor.EVENT_TYPE_HEADER).startsWith(CONV_CLASS)).to(DIRECT_CONVERSATION)
                    .when(header(WebhookEventTypeExtractor.EVENT_TYPE_HEADER).startsWith(PERSON_PREFIX)).to(DIRECT_PERSON)
                    .when(header(WebhookEventTypeExtractor.EVENT_TYPE_HEADER).startsWith(PERSON_CLASS)).to(DIRECT_PERSON)
                    .otherwise().to(DIRECT_UNKNOWN)
                .end();

        from(DIRECT_CONVERSATION).routeId("webhook-handle-conversation").process(conversationProcessor);
        from(DIRECT_PERSON).routeId("webhook-handle-person").process(personProcessor);
        from(DIRECT_UNKNOWN).routeId("webhook-handle-unknown").process(unknownProcessor);
    }
}
