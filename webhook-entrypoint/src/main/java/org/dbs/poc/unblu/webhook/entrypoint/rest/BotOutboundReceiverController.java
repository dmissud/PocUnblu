package org.dbs.poc.unblu.webhook.entrypoint.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Point d'entrée des outbound requests bot Unblu.
 *
 * <p>Reçoit le POST d'Unblu (forwardé via exposition:8081), publie un BotCommand sur
 * {@code unblu.bot.commands} et répond immédiatement avec la réponse synchrone attendue par Unblu.
 *
 * <p>Les events de boarding (onboarding/reboarding/offboarding) sont acquittés avec
 * {@code offerAccepted: true}. Les events de dialog sont acquittés avec un ACK générique.
 * Le traitement métier est entièrement asynchrone via Kafka → engine.
 */
@Slf4j
@RestController
public class BotOutboundReceiverController {

    private static final String SERVICE_ONBOARDING_OFFER = "outbound.bot.onboarding_offer";
    private static final String SERVICE_REBOARDING_OFFER  = "outbound.bot.reboarding_offer";
    private static final String SERVICE_OFFBOARDING_OFFER = "outbound.bot.offboarding_offer";
    private static final String SERVICE_DIALOG_OPENED     = "outbound.bot.dialog.opened";
    private static final String SERVICE_DIALOG_MESSAGE    = "outbound.bot.dialog.message";
    private static final String SERVICE_DIALOG_CLOSED     = "outbound.bot.dialog.closed";

    private static final Pattern DIALOG_TOKEN_PATTERN     = Pattern.compile("\"dialogToken\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CONVERSATION_ID_PATTERN  = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.bot-commands:unblu.bot.commands}")
    private String botCommandsTopic;

    public BotOutboundReceiverController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping(value = "/api/bot/outbound",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader("x-unblu-service-name") String serviceName,
            @RequestHeader(value = "x-bot-correlation-id", required = false, defaultValue = "-") String correlationId,
            @RequestBody(required = false) String rawBody) {

        log.info("[BOT_ENTRY] correlationId={} event={}", correlationId, serviceName);

        String commandType = toCommandType(serviceName);
        if (commandType != null && rawBody != null && !rawBody.isBlank()) {
            String dialogToken    = extract(DIALOG_TOKEN_PATTERN, rawBody);
            String conversationId = extractConversationId(rawBody);
            String command = buildCommand(commandType, correlationId, dialogToken, conversationId, rawBody);
            String kafkaKey = dialogToken != null ? dialogToken : UUID.randomUUID().toString();

            kafkaTemplate.send(botCommandsTopic, kafkaKey, command);
            log.info("[BOT_ENTRY] correlationId={} commandType={} conversationId={} published to {}",
                    correlationId, commandType, conversationId, botCommandsTopic);
        }

        return switch (serviceName) {
            case SERVICE_ONBOARDING_OFFER, SERVICE_REBOARDING_OFFER, SERVICE_OFFBOARDING_OFFER ->
                    ResponseEntity.ok(Map.of("$_type", "BotBoardingOfferResponse", "offerAccepted", true));
            case SERVICE_DIALOG_MESSAGE ->
                    ResponseEntity.ok(Map.of("$_type", "BotDialogMessageResponse"));
            case SERVICE_DIALOG_CLOSED ->
                    ResponseEntity.ok(Map.of("$_type", "BotDialogClosedResponse"));
            default ->
                    ResponseEntity.ok(Map.of("$_type", "BotDialogOpenResponse"));
        };
    }

    private String toCommandType(String serviceName) {
        return switch (serviceName) {
            case SERVICE_ONBOARDING_OFFER  -> "BOT_ONBOARDING_OFFER";
            case SERVICE_REBOARDING_OFFER  -> "BOT_REBOARDING_OFFER";
            case SERVICE_OFFBOARDING_OFFER -> "BOT_OFFBOARDING_OFFER";
            case SERVICE_DIALOG_OPENED     -> "BOT_DIALOG_OPENED";
            case SERVICE_DIALOG_MESSAGE    -> "BOT_DIALOG_MESSAGE";
            case SERVICE_DIALOG_CLOSED     -> "BOT_DIALOG_CLOSED";
            default -> null;
        };
    }

    private String buildCommand(String commandType, String correlationId,
                                String dialogToken, String conversationId, String rawPayload) {
        String escaped = rawPayload.replace("\\", "\\\\").replace("\"", "\\\"");
        return String.format(
                "{\"commandType\":\"%s\",\"correlationId\":\"%s\",\"dialogToken\":%s," +
                "\"conversationId\":%s,\"timestamp\":\"%s\",\"rawPayload\":\"%s\"}",
                commandType,
                correlationId,
                dialogToken != null ? "\"" + dialogToken + "\"" : "null",
                conversationId != null ? "\"" + conversationId + "\"" : "null",
                Instant.now().toString(),
                escaped
        );
    }

    private String extract(Pattern pattern, String json) {
        Matcher m = pattern.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** Extrait le conversationId depuis conversation.id dans le payload dialog.opened. */
    private String extractConversationId(String json) {
        int convIdx = json.indexOf("\"conversation\"");
        if (convIdx < 0) return null;
        Matcher m = CONVERSATION_ID_PATTERN.matcher(json.substring(convIdx));
        return m.find() ? m.group(1) : null;
    }
}
