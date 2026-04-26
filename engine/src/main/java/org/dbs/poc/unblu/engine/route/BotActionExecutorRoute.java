package org.dbs.poc.unblu.engine.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.integration.domain.model.bot.BotAction;
import org.dbs.poc.unblu.integration.domain.model.bot.BotActionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Route Camel consommant les actions bot depuis {@code unblu.bot.actions}
 * et exécutant les appels REST vers l'API Unblu.
 *
 * <p>Actions supportées :
 * <ul>
 *   <li>{@code SET_NAMED_AREA} → {@code conversationsSetRecipient()}</li>
 *   <li>{@code SEND_MESSAGE} → {@code botsSendDialogMessage()}</li>
 *   <li>{@code HAND_OFF} → {@code botsFinishDialog(HAND_OFF)}</li>
 * </ul>
 *
 * <p>Erreur Unblu API → 3 retries + backoff → DLQ {@code unblu.bot.actions.dlq}.
 */
@Slf4j
@Component
public class BotActionExecutorRoute extends RouteBuilder {

    private static final String ROUTE_ID = "bot-action-executor";
    private static final String ROUTE_ID_DLQ = "bot-action-dlq-producer";
    private static final String DIRECT_DLQ = "direct:bot-action-dlq";

    private final ObjectMapper objectMapper;
    private final ApiClient integrationUnbluApiClient;

    @Value("${kafka.topic.bot-actions:unblu.bot.actions}")
    private String topic;

    @Value("${kafka.topic.bot-actions-dlq:unblu.bot.actions.dlq}")
    private String dlqTopic;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id:unblu-engine}")
    private String groupId;

    public BotActionExecutorRoute(ObjectMapper objectMapper, ApiClient integrationUnbluApiClient) {
        this.objectMapper = objectMapper;
        this.integrationUnbluApiClient = integrationUnbluApiClient;
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
                        "Bot action to DLQ — error=${header.dlq.error.message}, retries=${header.dlq.retry.count}")
                .to("kafka:" + dlqTopic + "?brokers=" + bootstrapServers);
    }

    private void configureExceptionHandlers() {
        onException(IllegalArgumentException.class)
                .handled(true)
                .process(exchange -> enrichDlqHeaders(exchange, 0))
                .to(DIRECT_DLQ);

        onException(ApiException.class, Exception.class)
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
                        "Bot action received — topic=" + topic + ", key=${header.kafka.KEY}")
                .process(exchange -> {
                    String json = exchange.getIn().getBody(String.class);
                    if (json == null || json.isBlank()) {
                        throw new IllegalArgumentException("Empty bot action body");
                    }
                    BotAction action = objectMapper.readValue(json, BotAction.class);
                    exchange.getIn().setBody(action);

                    long t = System.currentTimeMillis();
                    switch (action.actionType()) {
                        case BotActionType.SET_NAMED_AREA -> executeSetNamedArea(action, t);
                        case BotActionType.SEND_MESSAGE   -> executeSendMessage(action, t);
                        case BotActionType.HAND_OFF       -> executeHandOff(action, t);
                        default -> log.warn("[BOT_EXEC] unknown actionType={} correlationId={}",
                                action.actionType(), action.correlationId());
                    }
                });
    }

    private void executeSetNamedArea(BotAction action, long start) throws ApiException {
        String namedAreaId = action.payload().get("namedAreaId");
        ConversationsApi api = new ConversationsApi(integrationUnbluApiClient);
        NamedAreaData recipient = new NamedAreaData();
        recipient.setId(namedAreaId);
        api.conversationsSetRecipient(action.conversationId(), recipient, null);
        log.info("[BOT_EXEC] step=SET_NAMED_AREA correlationId={} conversationId={} namedAreaId={} status=OK durationMs={}",
                action.correlationId(), action.conversationId(), namedAreaId, System.currentTimeMillis() - start);
    }

    private void executeSendMessage(BotAction action, long start) throws ApiException {
        String text = action.payload().get("text");
        BotsApi api = new BotsApi(integrationUnbluApiClient);
        api.botsSendDialogMessage(
                new BotDialogPostMessage()
                        .dialogToken(action.dialogToken())
                        .messageData(new TextPostMessageData()
                                .type(EPostMessageType.TEXT)
                                .text(text)
                                .fallbackText(text))
        );
        log.info("[BOT_EXEC] step=SEND_MESSAGE correlationId={} dialogToken={} textLength={} status=OK durationMs={}",
                action.correlationId(), action.dialogToken(), text.length(), System.currentTimeMillis() - start);
    }

    private void executeHandOff(BotAction action, long start) throws ApiException {
        BotsApi api = new BotsApi(integrationUnbluApiClient);
        api.botsFinishDialog(new BotsFinishDialogBody()
                .dialogToken(action.dialogToken())
                .reason(EBotDialogFinishReason.HAND_OFF));
        log.info("[BOT_EXEC] step=HAND_OFF correlationId={} dialogToken={} status=OK durationMs={}",
                action.correlationId(), action.dialogToken(), System.currentTimeMillis() - start);
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
