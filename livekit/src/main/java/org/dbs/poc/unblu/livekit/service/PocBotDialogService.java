package org.dbs.poc.unblu.livekit.service;

import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.port.out.ConversationSummaryPort;
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
    public void onDialogOpened(String dialogToken, String conversationId) {
        CompletableFuture.runAsync(() -> {
            if (conversationId != null && !namedAreaId.isBlank()) {
                setNamedAreaRecipient(conversationId);
            }
            String message = summaryPort.generateSummary(conversationId);
            sendTextMessage(dialogToken, message);
            handOffToAgent(dialogToken);
        }).exceptionally(ex -> {
            log.error("Erreur lors du traitement de dialog.opened pour dialogToken={}", dialogToken, ex);
            return null;
        });
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
        log.info("Dialog fermé : dialogToken={}", dialogToken);
    }

    /**
     * Termine le dialog bot avec raison HAND_OFF pour rendre la main à un agent.
     * Doit être appelé après que le bot a terminé son traitement.
     */
    private void handOffToAgent(String dialogToken) {
        try {
            BotsApi botsApi = new BotsApi(apiClient);
            botsApi.botsFinishDialog(new BotsFinishDialogBody()
                    .dialogToken(dialogToken)
                    .reason(EBotDialogFinishReason.HAND_OFF));
            log.info("Handoff vers agent effectué pour dialog {}", dialogToken);
        } catch (ApiException e) {
            log.error("Échec du handoff vers agent pour dialogToken={} : status={}", dialogToken, e.getCode(), e);
        }
    }

    private void setNamedAreaRecipient(String conversationId) {
        try {
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);
            NamedAreaData recipient = new NamedAreaData();
            recipient.setId(namedAreaId);
            conversationsApi.conversationsSetRecipient(conversationId, recipient, null);
            log.info("NamedArea {} positionné comme destinataire de la conversation {}", namedAreaId, conversationId);
        } catch (ApiException e) {
            log.error("Échec du positionnement du namedArea pour conversationId={} : status={}",
                    conversationId, e.getCode(), e);
        }
    }

    private void sendTextMessage(String dialogToken, String text) {
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
            log.info("Message envoyé dans le dialog {}", dialogToken);
        } catch (ApiException e) {
            log.error("Échec de l'envoi du message dans dialog={} : status={}", dialogToken, e.getCode(), e);
        }
    }
}
