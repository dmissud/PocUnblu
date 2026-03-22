package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.infrastructure.config.UnbluProperties;
import org.dbs.poc.unblu.infrastructure.exception.UnbluApiException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service d'accès à l'API Unblu pour les opérations de gestion des conversations :
 * création standard, création directe (1-à-1), et ajout d'un message de résumé via un bot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnbluConversationService {

    private final ApiClient apiClient;
    private final UnbluProperties unbluProperties;

    private static final List<ExpandFields> NO_EXPAND = null;

    /**
     * Crée une conversation Unblu à partir des données de création fournies.
     *
     * @param conversationData les données de création (sujet, type d'engagement, destinataire, participants)
     * @return les données de la conversation créée
     * @throws org.dbs.poc.unblu.infrastructure.exception.UnbluApiException en cas d'erreur API
     */
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

    /**
     * Crée une conversation directe (1-à-1) entre un participant virtuel et un agent Unblu.
     *
     * @param virtualPerson la personne virtuelle (visiteur) participant à la conversation
     * @param agentPerson   l'agent assigné ; doit être de type {@code AGENT}
     * @param subject       le sujet de la conversation
     * @return les données de la conversation directe créée
     * @throws org.dbs.poc.unblu.infrastructure.exception.UnbluApiException si l'assigné n'est pas un agent (400) ou en cas d'erreur API
     */
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

    /**
     * Retourne la liste de toutes les conversations présentes dans Unblu.
     *
     * @return liste des résumés de conversations mappés en objets domaine
     * @throws org.dbs.poc.unblu.infrastructure.exception.UnbluApiException en cas d'erreur API
     */
    public List<UnbluConversationSummary> listAllConversations() {
        try {
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);
            ConversationQuery query = new ConversationQuery();
            log.info("Récupération de toutes les conversations dans Unblu...");
            ConversationResult result = conversationsApi.conversationsSearch(query, NO_EXPAND);
            log.info("Trouvé {} conversation(s)", result.getItems().size());
            return result.getItems().stream()
                    .map(this::toConversationSummary)
                    .toList();
        } catch (ApiException e) {
            log.error("Erreur lors de la récupération des conversations - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error",
                    "Erreur lors de la récupération des conversations : " + e.getMessage());
        }
    }

    private UnbluConversationSummary toConversationSummary(ConversationData data) {
        Instant createdAt = data.getCreationTimestamp() != null
                ? Instant.ofEpochMilli(data.getCreationTimestamp())
                : Instant.now();
        Instant endedAt = data.getEndTimestamp() != null
                ? Instant.ofEpochMilli(data.getEndTimestamp())
                : null;
        String state = data.getState() != null ? data.getState().name() : "UNKNOWN";
        return new UnbluConversationSummary(data.getId(), data.getTopic(), state, createdAt, endedAt);
    }

    /**
     * Ajoute un message de résumé à une conversation Unblu existante via le bot configuré.
     * Si {@code unblu.api.summary-bot-person-id} n'est pas configuré, l'opération est ignorée.
     *
     * @param conversationId l'identifiant de la conversation cible
     * @param summary        le texte du résumé à envoyer
     * @throws org.dbs.poc.unblu.infrastructure.exception.UnbluApiException en cas d'erreur API lors de l'ajout du participant ou de l'envoi du message
     */
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
