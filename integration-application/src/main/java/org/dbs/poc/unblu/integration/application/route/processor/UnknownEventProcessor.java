package org.dbs.poc.unblu.integration.application.route.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.dbs.poc.unblu.integration.domain.model.webhook.UnbluWebhookPayload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UnknownEventProcessor implements Processor {
    @Override
    public void process(Exchange exchange) {
        UnbluWebhookPayload payload = exchange.getIn().getBody(UnbluWebhookPayload.class);
        log.warn("Unknown webhook event — type={}, eventType={}", payload.type(), payload.eventType());
    }
}
