package org.dbs.poc.unblu.livekit.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.livekit.service.PocBotDialogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Point d'entrée des outbound requests Unblu pour le bot PocBot.
 *
 * <p>Unblu appelle ce endpoint de façon synchrone et attend une réponse dans le délai configuré.
 * Les décisions de boarding (onboarding/offboarding) sont traitées directement.
 * Les événements de dialog retournent un acquittement immédiat, le traitement métier
 * est délégué à {@link PocBotDialogService} de façon asynchrone.
 */
@Slf4j
@RestController
@RequestMapping("/api/bot/outbound")
@RequiredArgsConstructor
@Tag(name = "Bot Outbound", description = "Outbound requests Unblu pour le bot PocBot")
public class BotOutboundController {

    private static final String SERVICE_ONBOARDING_OFFER  = "outbound.bot.onboarding_offer";
    private static final String SERVICE_REBOARDING_OFFER   = "outbound.bot.reboarding_offer";
    private static final String SERVICE_OFFBOARDING_OFFER  = "outbound.bot.offboarding_offer";
    private static final String SERVICE_DIALOG_OPENED      = "outbound.bot.dialog.opened";
    private static final String SERVICE_DIALOG_MESSAGE     = "outbound.bot.dialog.message";
    private static final String SERVICE_DIALOG_CLOSED      = "outbound.bot.dialog.closed";

    private final ObjectMapper objectMapper;
    private final PocBotDialogService pocBotDialogService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reçoit les outbound requests Unblu pour PocBot")
    public ResponseEntity<Map<String, Object>> handle(
            @RequestHeader("x-unblu-service-name") String serviceName,
            @RequestHeader(value = "x-bot-correlation-id", required = false, defaultValue = "-") String correlationId,
            @RequestBody String rawBody) {

        log.info("[BOT_EVENT] correlationId={} event={}", correlationId, serviceName);

        return switch (serviceName) {
            case SERVICE_ONBOARDING_OFFER, SERVICE_REBOARDING_OFFER -> acceptBoarding(correlationId);
            case SERVICE_OFFBOARDING_OFFER                          -> acceptOffboarding(correlationId);
            case SERVICE_DIALOG_OPENED                              -> handleDialogOpened(rawBody, correlationId);
            case SERVICE_DIALOG_MESSAGE                             -> handleDialogMessage(rawBody, correlationId);
            case SERVICE_DIALOG_CLOSED                              -> handleDialogClosed(rawBody, correlationId);
            default -> {
                log.warn("[BOT_EVENT] correlationId={} event={} action=IGNORED (unknown service-name)",
                        correlationId, serviceName);
                yield ack();
            }
        };
    }

    // --- Boarding decisions (synchrones) ---

    private ResponseEntity<Map<String, Object>> acceptBoarding(String correlationId) {
        log.info("[BOT_EVENT] correlationId={} event=onboarding_offer action=ACCEPTED", correlationId);
        return ResponseEntity.ok(Map.of(
                "$_type", "BotBoardingOfferResponse",
                "offerAccepted", true
        ));
    }

    private ResponseEntity<Map<String, Object>> acceptOffboarding(String correlationId) {
        log.info("[BOT_EVENT] correlationId={} event=offboarding_offer action=ACCEPTED", correlationId);
        return ResponseEntity.ok(Map.of(
                "$_type", "BotBoardingOfferResponse",
                "offerAccepted", true
        ));
    }

    // --- Dialog events (acquittement immédiat + traitement async) ---

    private ResponseEntity<Map<String, Object>> handleDialogOpened(String rawBody, String correlationId) {
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            String dialogToken    = node.path("dialogToken").asText();
            String conversationId = node.path("conversation").path("id").asText(null);
            log.info("[BOT_EVENT] correlationId={} event=dialog.opened dialogToken={} conversationId={}",
                    correlationId, dialogToken, conversationId);
            pocBotDialogService.onDialogOpened(dialogToken, conversationId, correlationId);
        } catch (Exception e) {
            log.error("[BOT_EVENT] correlationId={} event=dialog.opened action=PARSE_ERROR", correlationId, e);
        }
        return ack();
    }

    private ResponseEntity<Map<String, Object>> handleDialogMessage(String rawBody, String correlationId) {
        try {
            JsonNode node          = objectMapper.readTree(rawBody);
            String dialogToken     = node.path("dialogToken").asText();
            String conversationId  = node.path("conversationId").asText();
            String fallbackText    = node.path("conversationMessage").path("fallbackText").asText();
            String senderType      = node.path("conversationMessage").path("senderPerson").path("type").asText();
            log.info("[BOT_EVENT] correlationId={} event=dialog.message dialogToken={} senderType={} textLength={}",
                    correlationId, dialogToken, senderType, fallbackText.length());
            pocBotDialogService.onDialogMessage(dialogToken, conversationId, fallbackText, senderType);
        } catch (Exception e) {
            log.error("[BOT_EVENT] correlationId={} event=dialog.message action=PARSE_ERROR", correlationId, e);
        }
        return ResponseEntity.ok(Map.of("$_type", "BotDialogMessageResponse"));
    }

    private ResponseEntity<Map<String, Object>> handleDialogClosed(String rawBody, String correlationId) {
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            String dialogToken = node.path("dialogToken").asText();
            log.info("[BOT_EVENT] correlationId={} event=dialog.closed dialogToken={}", correlationId, dialogToken);
            pocBotDialogService.onDialogClosed(dialogToken);
        } catch (Exception e) {
            log.error("[BOT_EVENT] correlationId={} event=dialog.closed action=PARSE_ERROR", correlationId, e);
        }
        return ResponseEntity.ok(Map.of("$_type", "BotDialogClosedResponse"));
    }

    private ResponseEntity<Map<String, Object>> ack() {
        return ResponseEntity.ok(Map.of("$_type", "BotDialogOpenResponse"));
    }
}
