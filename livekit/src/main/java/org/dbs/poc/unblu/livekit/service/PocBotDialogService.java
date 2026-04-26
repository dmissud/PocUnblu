package org.dbs.poc.unblu.livekit.service;

import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationSummaryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service de gestion du dialogue du bot PocBot.
 * Les actions sur le dialogue (envoi de message, positionnement du namedArea)
 * sont exécutées de façon asynchrone afin de retourner immédiatement l'acquittement à Unblu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PocBotDialogService {

    private final ApiClient apiClient;
    private final ConversationSummaryPort summaryPort;

    @Value("${unblu.bot.named-area-id:}")
    private String namedAreaId;

    /**
     * Réagit à l'ouverture d'un dialog bot.
     * Exécuté de façon asynchrone : positionne le namedArea puis envoie le message de bienvenue.
     *
     * @param dialogToken    token du dialog Unblu
     * @param conversationId identifiant de la conversation
     */
    public void onDialogOpened(String dialogToken, String conversationId, String correlationId) {
        long asyncStart = System.currentTimeMillis();
        log.info("[BOT_TRACE] step=ASYNC_START correlationId={} dialogToken={} conversationId={}",
                correlationId, dialogToken, conversationId);

        CompletableFuture.runAsync(() -> {
            if (conversationId != null && !namedAreaId.isBlank()) {
                setNamedAreaRecipient(conversationId, correlationId);
            }
            String message = summaryPort.generateSummary(conversationId);
            log.info("[BOT_TRACE] step=SUMMARY_GENERATED correlationId={} dialogToken={} summaryLength={}",
                    correlationId, dialogToken, message != null ? message.length() : 0);
            sendTextMessage(dialogToken, message, correlationId);
            handOffToAgent(dialogToken, correlationId);
            log.info("[BOT_TRACE] step=ASYNC_DONE correlationId={} dialogToken={} totalDurationMs={}",
                    correlationId, dialogToken, System.currentTimeMillis() - asyncStart);
        }).exceptionally(ex -> {
            log.error("[BOT_TRACE] step=ASYNC_ERROR correlationId={} dialogToken={} error={}",
                    correlationId, dialogToken, ex.getMessage(), ex);
            return null;
        });
    }

    /** @deprecated use {@link #onDialogOpened(String, String, String)} */
    public void onDialogOpened(String dialogToken, String conversationId) {
        onDialogOpened(dialogToken, conversationId, "-");
    }

    /**
     * Réagit à la réception d'un message dans le dialog.
     * Seuls les messages des visiteurs sont traités.
     *
     * @param dialogToken    token du dialog Unblu
     * @param conversationId identifiant de la conversation
     * @param fallbackText   texte du message
     * @param senderType     type d'émetteur (VISITOR, AGENT, BOT, SYSTEM)
     */
    public void onDialogMessage(String dialogToken, String conversationId,
                                String fallbackText, String senderType) {
        if (!"VISITOR".equals(senderType)) {
            return;
        }
        log.info("Message visiteur reçu dans dialog={} : {}", dialogToken, fallbackText);
        // Logique métier à enrichir selon les besoins
    }

    /**
     * Réagit à la fermeture d'un dialog.
     *
     * @param dialogToken token du dialog fermé
     */
    public void onDialogClosed(String dialogToken) {
        log.info("[BOT_EVENT] step=DIALOG_CLOSED dialogToken={}", dialogToken);
    }

    private void handOffToAgent(String dialogToken, String correlationId) {
        long t = System.currentTimeMillis();
        try {
            BotsApi botsApi = new BotsApi(apiClient);
            botsApi.botsFinishDialog(new BotsFinishDialogBody()
                    .dialogToken(dialogToken)
                    .reason(EBotDialogFinishReason.HAND_OFF));
            log.info("[BOT_TRACE] step=HAND_OFF correlationId={} dialogToken={} status=OK durationMs={}",
                    correlationId, dialogToken, System.currentTimeMillis() - t);
        } catch (ApiException e) {
            log.error("[BOT_TRACE] step=HAND_OFF correlationId={} dialogToken={} status=ERROR httpStatus={} durationMs={}",
                    correlationId, dialogToken, e.getCode(), System.currentTimeMillis() - t, e);
        }
    }

    private void setNamedAreaRecipient(String conversationId, String correlationId) {
        long t = System.currentTimeMillis();
        try {
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);
            NamedAreaData recipient = new NamedAreaData();
            recipient.setId(namedAreaId);
            conversationsApi.conversationsSetRecipient(conversationId, recipient, null);
            log.info("[BOT_TRACE] step=SET_NAMED_AREA correlationId={} conversationId={} namedAreaId={} status=OK durationMs={}",
                    correlationId, conversationId, namedAreaId, System.currentTimeMillis() - t);
        } catch (ApiException e) {
            log.error("[BOT_TRACE] step=SET_NAMED_AREA correlationId={} conversationId={} status=ERROR httpStatus={} durationMs={}",
                    correlationId, conversationId, e.getCode(), System.currentTimeMillis() - t, e);
        }
    }

    private void sendTextMessage(String dialogToken, String text, String correlationId) {
        long t = System.currentTimeMillis();
        try {
            BotsApi botsApi = new BotsApi(apiClient);
            botsApi.botsSendDialogMessage(
                    new BotDialogPostMessage()
                            .dialogToken(dialogToken)
                            .messageData(new TextPostMessageData()
                                    .type(EPostMessageType.TEXT)
                                    .text(text)
                                    .fallbackText(text))
            );
            log.info("[BOT_TRACE] step=SEND_MESSAGE correlationId={} dialogToken={} status=OK durationMs={}",
                    correlationId, dialogToken, System.currentTimeMillis() - t);
        } catch (ApiException e) {
            log.error("[BOT_TRACE] step=SEND_MESSAGE correlationId={} dialogToken={} status=ERROR httpStatus={} durationMs={}",
                    correlationId, dialogToken, e.getCode(), System.currentTimeMillis() - t, e);
        }
    }
}
