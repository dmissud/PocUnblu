package org.dbs.poc.unblu.livekit.service;

import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.ClientContext;
import org.dbs.poc.unblu.integration.domain.port.out.ClientContextPort;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationSummaryPort;
import org.dbs.poc.unblu.integration.domain.port.out.NamedAreaResolverPort;
import org.dbs.poc.unblu.integration.domain.port.out.ResourceUrlPort;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service de gestion du dialogue du bot PocBot.
 *
 * <p>À l'ouverture d'un dialog, orchestre de façon asynchrone 3 appels vers des systèmes externes
 * (simulés par des mocks), puis positionne les résultats dans Unblu avant de rendre la main à un agent :
 * <ol>
 *   <li>CRM → résolution du contexte client (segment, langue)</li>
 *   <li>LLM → génération du résumé de conversation</li>
 *   <li>Moteur de règles → résolution du named area cible + positionnement dans Unblu</li>
 *   <li>Service documentaire → calcul d'une URL contextuelle + envoi au visiteur</li>
 *   <li>HAND_OFF → transfert vers les agents</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PocBotDialogService {

    private final ApiClient apiClient;
    private final ConversationSummaryPort summaryPort;
    private final ClientContextPort clientContextPort;
    private final NamedAreaResolverPort namedAreaResolverPort;
    private final ResourceUrlPort resourceUrlPort;

    public void onDialogOpened(String dialogToken, String conversationId, String correlationId) {
        long asyncStart = System.currentTimeMillis();
        log.info("[BOT_TRACE] step=ASYNC_START correlationId={} dialogToken={} conversationId={}",
                correlationId, dialogToken, conversationId);

        CompletableFuture.runAsync(() -> {

            // --- Étape 1 : résolution du contexte client (CRM) ---
            long t1 = System.currentTimeMillis();
            ClientContext clientContext = clientContextPort.resolveClientContext(conversationId);
            log.info("[BOT_TRACE] step=CLIENT_CONTEXT correlationId={} clientId={} segment={} language={} durationMs={}",
                    correlationId, clientContext.clientId(), clientContext.segment(),
                    clientContext.language(), System.currentTimeMillis() - t1);

            // --- Étape 2 : génération du résumé (LLM) ---
            long t2 = System.currentTimeMillis();
            String summary = summaryPort.generateSummary(conversationId);
            log.info("[BOT_TRACE] step=SUMMARY_GENERATED correlationId={} summaryLength={} durationMs={}",
                    correlationId, summary != null ? summary.length() : 0, System.currentTimeMillis() - t2);

            // --- Étape 3 : résolution du named area + positionnement dans Unblu ---
            long t3 = System.currentTimeMillis();
            String namedAreaId = namedAreaResolverPort.resolveNamedAreaId(clientContext);
            log.info("[BOT_TRACE] step=NAMED_AREA_RESOLVED correlationId={} namedAreaId={} durationMs={}",
                    correlationId, namedAreaId, System.currentTimeMillis() - t3);
            setNamedAreaRecipient(conversationId, namedAreaId, correlationId);

            // --- Étape 4 : calcul de l'URL contextuelle + envoi au visiteur ---
            long t4 = System.currentTimeMillis();
            String resourceUrl = resourceUrlPort.computeResourceUrl(clientContext);
            log.info("[BOT_TRACE] step=URL_COMPUTED correlationId={} url={} durationMs={}",
                    correlationId, resourceUrl, System.currentTimeMillis() - t4);
            sendTextMessage(dialogToken, buildMessage(summary, resourceUrl), correlationId);

            // --- Étape 5 : HAND_OFF ---
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

    public void onDialogMessage(String dialogToken, String conversationId,
                                String fallbackText, String senderType) {
        if (!"VISITOR".equals(senderType)) {
            return;
        }
        log.info("[BOT_EVENT] step=VISITOR_MESSAGE dialogToken={} text={}", dialogToken, fallbackText);
    }

    public void onDialogClosed(String dialogToken) {
        log.info("[BOT_EVENT] step=DIALOG_CLOSED dialogToken={}", dialogToken);
    }

    // --- Appels Unblu API ---

    private void setNamedAreaRecipient(String conversationId, String namedAreaId, String correlationId) {
        long t = System.currentTimeMillis();
        try {
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);
            NamedAreaData recipient = new NamedAreaData();
            recipient.setId(namedAreaId);
            conversationsApi.conversationsSetRecipient(conversationId, recipient, null);
            log.info("[BOT_TRACE] step=SET_NAMED_AREA correlationId={} conversationId={} namedAreaId={} status=OK durationMs={}",
                    correlationId, conversationId, namedAreaId, System.currentTimeMillis() - t);
        } catch (ApiException e) {
            log.error("[BOT_TRACE] step=SET_NAMED_AREA correlationId={} conversationId={} namedAreaId={} status=ERROR httpStatus={} durationMs={}",
                    correlationId, conversationId, namedAreaId, e.getCode(), System.currentTimeMillis() - t, e);
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
            log.info("[BOT_TRACE] step=SEND_MESSAGE correlationId={} dialogToken={} textLength={} status=OK durationMs={}",
                    correlationId, dialogToken, text.length(), System.currentTimeMillis() - t);
        } catch (ApiException e) {
            log.error("[BOT_TRACE] step=SEND_MESSAGE correlationId={} dialogToken={} status=ERROR httpStatus={} durationMs={}",
                    correlationId, dialogToken, e.getCode(), System.currentTimeMillis() - t, e);
        }
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

    private String buildMessage(String summary, String resourceUrl) {
        return summary + "\n\nVotre espace personnel : " + resourceUrl;
    }
}
