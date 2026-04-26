package org.dbs.poc.unblu.engine.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.integration.domain.model.bot.BotCommand;
import org.dbs.poc.unblu.integration.domain.model.bot.BotCommandType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Route Camel consommant les commandes bot depuis le topic {@code unblu.bot.commands}.
 *
 * <p>Filtre les événements pertinents : seul {@code BOT_DIALOG_OPENED} déclenche
 * le traitement métier via {@code direct:bot-dialog-processor}.
 *
 * <p>Error handling identique à {@link KafkaWebhookConsumerRoute} :
 * payload invalide → DLQ immédiat, erreur transitoire → 3 retries + backoff → DLQ.
 */
@Slf4j
@Component
public class BotCommandConsumerRoute extends RouteBuilder {

    private static final String ROUTE_ID = "bot-command-consumer";
    private static final String ROUTE_ID_DLQ = "bot-command-dlq-producer";
    static final String DIRECT_BOT_DIALOG_PROCESSOR = "direct:bot-dialog-processor";
    private static final String DIRECT_DLQ = "direct:bot-command-dlq";

    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.bot-commands:unblu.bot.commands}")
    private String topic;

    @Value("${kafka.topic.bot-commands-dlq:unblu.bot.actions.dlq}")
    private String dlqTopic;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id:unblu-engine}")
    private String groupId;

    public BotCommandConsumerRoute(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void configure() {
        configureExceptionHandlers();
        configureDlqRoute();
        configureConsumerRoute();
    }

    private void configureDlqRoute() {
        from(DIRECT_DLQ)
                .routeId(ROUTE_ID_DLQ)
                .log(LoggingLevel.ERROR, ROUTE_ID_DLQ,
                        "Bot command to DLQ — error=${header.dlq.error.message}, retries=${header.dlq.retry.count}")
                .to("kafka:" + dlqTopic + "?brokers=" + bootstrapServers)
                .log(LoggingLevel.WARN, ROUTE_ID_DLQ, "Bot command parked in DLQ: " + dlqTopic);
    }

    private void configureExceptionHandlers() {
        onException(IllegalArgumentException.class)
                .handled(true)
                .process(exchange -> enrichDlqHeaders(exchange, 0))
                .to(DIRECT_DLQ);

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
                        "Bot command received — topic=" + topic + ", key=${header.kafka.KEY}")
                .process(exchange -> {
                    String json = exchange.getIn().getBody(String.class);
                    if (json == null || json.isBlank()) {
                        throw new IllegalArgumentException("Empty bot command body");
                    }
                    BotCommand command = objectMapper.readValue(json, BotCommand.class);
                    exchange.getIn().setBody(command);
                    log.info("[BOT_CMD] commandType={} correlationId={} conversationId={}",
                            command.commandType(), command.correlationId(), command.conversationId());
                })
                .choice()
                    .when(exchange -> BotCommandType.BOT_DIALOG_OPENED.equals(
                            exchange.getIn().getBody(BotCommand.class).commandType()))
                        .to(DIRECT_BOT_DIALOG_PROCESSOR)
                    .otherwise()
                        .log(LoggingLevel.DEBUG, ROUTE_ID,
                                "Bot command ignored — commandType=${body.commandType}")
                .end();
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
