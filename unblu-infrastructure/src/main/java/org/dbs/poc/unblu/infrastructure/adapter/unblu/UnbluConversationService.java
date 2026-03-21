package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.infrastructure.config.UnbluProperties;
import org.dbs.poc.unblu.infrastructure.exception.UnbluApiException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles Unblu conversation operations: create, create direct, add summary message.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnbluConversationService {

    private final ApiClient apiClient;
    private final UnbluProperties unbluProperties;

    private static final List<ExpandFields> NO_EXPAND = null;

    public ConversationData createConversation(ConversationCreationData conversationData) {
        try {
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);
            log.info("Creating conversation in Unblu...");
            ConversationData result = conversationsApi.conversationsCreate(conversationData, NO_EXPAND);
            log.info("Conversation created with ID: {}", result.getId());
            return result;
        } catch (ApiException e) {
            log.error("Error creating conversation - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes pour créer une conversation");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création de la conversation : " + e.getMessage());
        }
    }

    public ConversationData createDirectConversation(PersonInfo virtualPerson, PersonInfo agentPerson, String subject) {
        try {
            if (agentPerson == null || !agentPerson.isAgent()) {
                String errorMsg = String.format("L'assigné doit être un agent. Person ID: %s, Type: %s",
                        agentPerson != null ? agentPerson.id() : "null",
                        agentPerson != null ? agentPerson.personType() : "null");
                log.error(errorMsg);
                throw new UnbluApiException(400, "Bad Request", errorMsg);
            }

            ConversationsApi conversationsApi = new ConversationsApi(apiClient);

            ConversationCreationData data = new ConversationCreationData();
            data.setTopic(subject);
            data.setInitialEngagementType(EInitialEngagementType.CHAT_REQUEST);
            data.setVisitorData(virtualPerson.sourceId());

            ConversationCreationParticipantData virtualParticipant = new ConversationCreationParticipantData();
            virtualParticipant.setParticipationType(EConversationRealParticipationType.CONTEXT_PERSON);
            virtualParticipant.setPersonId(virtualPerson.id());

            ConversationCreationParticipantData agentParticipant = new ConversationCreationParticipantData();
            agentParticipant.setParticipationType(EConversationRealParticipationType.ASSIGNED_AGENT);
            agentParticipant.setPersonId(agentPerson.id());

            data.addParticipantsItem(virtualParticipant);
            data.addParticipantsItem(agentParticipant);

            log.info("Création d'une conversation directe - VIRTUAL: {}, Agent: {}", virtualPerson.id(), agentPerson.id());
            ConversationData result = conversationsApi.conversationsCreate(data, NO_EXPAND);
            log.info("Conversation directe créée avec ID: {}", result.getId());
            return result;
        } catch (ApiException e) {
            log.error("Erreur lors de la création de la conversation directe - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création de la conversation directe : " + e.getMessage());
        }
    }

    public void addSummaryToConversation(String conversationId, String summary) {
        try {
            String botPersonId = unbluProperties.getSummaryBotPersonId();
            if (botPersonId == null || botPersonId.isBlank()) {
                log.warn("unblu.api.summary-bot-person-id non configuré — résumé non envoyé dans la conversation {}", conversationId);
                return;
            }

            ConversationsApi conversationsApi = new ConversationsApi(apiClient);
            ConversationsAddParticipantBody addParticipantBody = new ConversationsAddParticipantBody();
            addParticipantBody.setPersonId(botPersonId);
            addParticipantBody.setHidden(true);
            conversationsApi.conversationsAddParticipant(conversationId, addParticipantBody, NO_EXPAND);

            TextPostMessageData messageData = new TextPostMessageData();
            messageData.setType(EPostMessageType.TEXT);
            messageData.setText(summary);
            messageData.setFallbackText(summary);

            BotPostMessage message = new BotPostMessage();
            message.setConversationId(conversationId);
            message.setSenderPersonId(botPersonId);
            message.setMessageData(messageData);

            BotsApi botsApi = new BotsApi(apiClient);
            log.info("Envoi du résumé comme message bot dans la conversation {}", conversationId);
            botsApi.botsSendMessage(message);
            log.info("Résumé envoyé avec succès dans la conversation: {}", conversationId);
        } catch (ApiException e) {
            log.error("Erreur lors de l'envoi du résumé dans la conversation {} - Status: {}", conversationId, e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de l'envoi du résumé : " + e.getMessage());
        }
    }
}
