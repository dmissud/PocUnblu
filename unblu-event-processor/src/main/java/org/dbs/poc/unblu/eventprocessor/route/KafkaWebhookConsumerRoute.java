package org.dbs.poc.unblu.eventprocessor.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.integration.domain.model.webhook.UnbluWebhookPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Camel route consuming raw JSON webhook events from Kafka topic {@code unblu.webhook.events}.
 *
 * <p>Deserializes the JSON payload into {@link UnbluWebhookPayload} and forwards to
 * {@code direct:webhook-event-processor} — the existing processing route in unblu-application.
 *
 * <p>Error handling:
 * <ul>
 *   <li>Blank/unparseable payload (permanent) → DLQ immédiatement, sans retry</li>
 *   <li>Erreur transitoire → 3 retries avec back-off exponentiel, puis DLQ</li>
 * </ul>
 *
 * <p>Les messages en DLQ sont enrichis avec les headers :
 * <ul>
 *   <li>{@code dlq.original.topic} — topic source</li>
 *   <li>{@code dlq.error.message} — message d'erreur</li>
 *   <li>{@code dlq.error.class} — classe de l'exception</li>
 *   <li>{@code dlq.failed.at} — timestamp ISO-8601 de l'échec</li>
 *   <li>{@code dlq.retry.count} — nombre de tentatives effectuées</li>
 * </ul>
 */
@Slf4j
@Component
public class KafkaWebhookConsumerRoute extends RouteBuilder {

    private static final String ROUTE_ID = "kafka-webhook-consumer";
    private static final String ROUTE_ID_DLQ = "kafka-webhook-dlq-producer";
    private static final String DIRECT_WEBHOOK_EVENT_PROCESSOR = "direct:webhook-event-processor";
    private static final String DIRECT_DLQ = "direct:webhook-dlq";

    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.webhook-events:unblu.webhook.events}")
    private String topic;

    @Value("${kafka.topic.webhook-events-dlq:unblu.webhook.events.dlq}")
    private String dlqTopic;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id:unblu-event-processor}")
    private String groupId;

    public KafkaWebhookConsumerRoute(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void configure() {
        configureExceptionHandlers();
        configureDlqRoute();
        configureConsumerRoute();
    }

    /**
     * Route DLQ : enrichit le message avec les métadonnées d'erreur et publie sur le topic DLQ.
     */
    private void configureDlqRoute() {
        from(DIRECT_DLQ)
                .routeId(ROUTE_ID_DLQ)
                .log(LoggingLevel.ERROR, ROUTE_ID_DLQ,
                        "Sending to DLQ — error=${header.dlq.error.message}, retries=${header.dlq.retry.count}")
                .to("kafka:" + dlqTopic + "?brokers=" + bootstrapServers)
                .log(LoggingLevel.WARN, ROUTE_ID_DLQ, "Message parked in DLQ topic: " + dlqTopic);
    }

    /**
     * Erreurs permanentes (payload invalide) → DLQ sans retry.
     * Erreurs transitoires → 3 retries avec back-off exponentiel, puis DLQ.
     */
    private void configureExceptionHandlers() {
        // Payload corrompu ou vide — inutile de réessayer
        onException(IllegalArgumentException.class)
                .handled(true)
                .process(exchange -> enrichDlqHeaders(exchange, 0))
                .log(LoggingLevel.ERROR, ROUTE_ID,
                        "Permanent error — invalid payload sent to DLQ: ${exception.message}")
                .to(DIRECT_DLQ);

        // Erreur transitoire (infra, service externe) — retry puis DLQ
        onException(Exception.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(2000)
                .backOffMultiplier(2)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .handled(true)
                .process(exchange -> {
                    Integer retryCount = exchange.getIn()
                            .getHeader(Exchange.REDELIVERY_COUNTER, Integer.class);
                    enrichDlqHeaders(exchange, retryCount != null ? retryCount : 3);
                })
                .log(LoggingLevel.ERROR, ROUTE_ID,
                        "Transient error after ${header.dlq.retry.count} retries — sending to DLQ: ${exception.message}")
                .to(DIRECT_DLQ);
    }

    private void configureConsumerRoute() {
        from("kafka:" + topic
                + "?brokers=" + bootstrapServers
                + "&groupId=" + groupId
                + "&autoOffsetReset=earliest"
                + "&autoCommitEnable=true")
                .routeId(ROUTE_ID)
                .log(LoggingLevel.INFO, ROUTE_ID,
                        "Kafka event received — topic=" + topic + ", key=${header.kafka.KEY}")
                .process(exchange -> {
                    String json = exchange.getIn().getBody(String.class);
                    if (json == null || json.isBlank()) {
                        throw new IllegalArgumentException("Empty Kafka message body");
                    }
                    UnbluWebhookPayload payload = objectMapper.readValue(json, UnbluWebhookPayload.class);
                    exchange.getIn().setBody(payload);

                    if (payload.eventType() != null) {
                        exchange.getIn().setHeader("X-Unblu-Event-Type", payload.eventType());
                    } else if (payload.type() != null) {
                        exchange.getIn().setHeader("X-Unblu-Event-Type", payload.type());
                    }

                    log.info("Deserialized webhook event — type={}, eventType={}",
                            payload.type(), payload.eventType());
                })
                .to(DIRECT_WEBHOOK_EVENT_PROCESSOR)
                .log(LoggingLevel.INFO, ROUTE_ID, "Webhook event processed successfully");
    }

    private void enrichDlqHeaders(Exchange exchange, int retryCount) {
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        exchange.getIn().setHeader("dlq.original.topic", topic);
        exchange.getIn().setHeader("dlq.error.message",
                cause != null ? cause.getMessage() : "unknown");
        exchange.getIn().setHeader("dlq.error.class",
                cause != null ? cause.getClass().getName() : "unknown");
        exchange.getIn().setHeader("dlq.failed.at", Instant.now().toString());
        exchange.getIn().setHeader("dlq.retry.count", retryCount);
    }
}
