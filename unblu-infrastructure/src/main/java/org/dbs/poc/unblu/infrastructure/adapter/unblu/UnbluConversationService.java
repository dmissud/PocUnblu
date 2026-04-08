package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.ConversationHistoryApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.api.InvitationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.domain.model.UnbluMessageData;
import org.dbs.poc.unblu.domain.model.UnbluParticipantData;
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

    private static final int PAGE_SIZE = 100;

    private static final int MESSAGE_PAGE_SIZE = 100;

    /**
     * Retourne la liste de toutes les conversations présentes dans Unblu.
     * Parcourt toutes les pages via {@code hasMoreItems} / {@code nextOffset}
     * pour dépasser la limite par défaut de l'API (250 items).
     *
     * @return liste des résumés de conversations mappés en objets domaine
     * @throws org.dbs.poc.unblu.infrastructure.exception.UnbluApiException en cas d'erreur API
     */
    public List<UnbluConversationSummary> listAllConversations() {
        try {
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);
            List<ConversationData> all = new java.util.ArrayList<>();
            int offset = 0;
            boolean hasMore = true;
            int pageNum = 0;
            long startTime = System.currentTimeMillis();

            log.info("Récupération de toutes les conversations dans Unblu (page size: {})...", PAGE_SIZE);
            while (hasMore) {
                long pageStart = System.currentTimeMillis();
                ConversationQuery query = new ConversationQuery();
                query.setOffset(offset);
                query.setLimit(PAGE_SIZE);
                ConversationResult result = conversationsApi.conversationsSearch(query, NO_EXPAND);
                all.addAll(result.getItems());
                hasMore = Boolean.TRUE.equals(result.isHasMoreItems());
                offset = hasMore ? result.getNextOffset() : offset;
                log.info("Page {} chargée en {}ms — {} conversation(s), hasMore: {}, nextOffset: {}",
                        ++pageNum, System.currentTimeMillis() - pageStart,
                        result.getItems().size(), hasMore, result.getNextOffset());
            }

            log.info("Trouvé {} conversation(s) au total en {}ms", all.size(), System.currentTimeMillis() - startTime);
            return all.stream()
                    .map(this::toConversationSummary)
                    .toList();
        } catch (ApiException e) {
            log.error("Erreur lors de la récupération des conversations - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error",
                    "Erreur lors de la récupération des conversations : " + e.getMessage());
        }
    }

    /**
     * Récupère tous les messages d'une conversation via {@code ConversationHistoryApi.conversationHistoryExportMessageLog}.
     * Parcourt toutes les pages jusqu'à épuisement des résultats.
     *
     * @param conversationId identifiant Unblu de la conversation
     * @return liste ordonnée des messages texte (les messages non-texte ou supprimés sont filtrés)
     */
    public List<UnbluMessageData> fetchMessages(String conversationId) {
        try {
            ConversationHistoryApi historyApi = new ConversationHistoryApi(apiClient);
            List<UnbluMessageData> all = new java.util.ArrayList<>();
            int offset = 0;
            boolean hasMore = true;

            log.info("Récupération des messages de la conversation {} ...", conversationId);
            while (hasMore) {
                MessageExportQuery query = new MessageExportQuery();
                query.setOffset(offset);
                query.setLimit(MESSAGE_PAGE_SIZE);
                MessageExportResult result = historyApi.conversationHistoryExportMessageLog(conversationId, query);
                if (result.getItems() != null) {
                    result.getItems().stream()
                            .filter(m -> m.getText() != null && m.getDeletedForAll() == null)
                            .forEach(m -> all.add(new UnbluMessageData(
                                    m.getId(),
                                    m.getText(),
                                    m.getSenderPersonId(),
                                    m.getSendTimestamp() != null
                                            ? Instant.ofEpochMilli(m.getSendTimestamp())
                                            : Instant.now())));
                }
                hasMore = Boolean.TRUE.equals(result.isHasMoreItems());
                offset = hasMore ? result.getNextOffset() : offset;
            }
            log.info("  {} message(s) récupéré(s) pour la conversation {}", all.size(), conversationId);
            return all;
        } catch (ApiException e) {
            log.error("Erreur lors de la récupération des messages de {} - Status: {}", conversationId, e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error",
                    "Erreur lors de la récupération des messages : " + e.getMessage());
        }
    }

    /**
     * Récupère les participants d'une conversation depuis {@code ConversationHistoryApi.conversationHistoryRead}.
     *
     * @param conversationId identifiant Unblu de la conversation
     * @return liste des participants avec leur nom d'affichage
     */
    public List<UnbluParticipantData> fetchParticipants(String conversationId) {
        try {
            ConversationHistoryApi historyApi = new ConversationHistoryApi(apiClient);
            ConversationHistoryData data = historyApi.conversationHistoryRead(conversationId, NO_EXPAND);
            if (data.getParticipants() == null) {
                return java.util.List.of();
            }
            return data.getParticipants().stream()
                    .filter(p -> p.getPerson() != null)
                    .map(p -> {
                        String displayName = p.getPerson().getDisplayName() != null
                                ? p.getPerson().getDisplayName()
                                : p.getPerson().getId();
                        String personSource = p.getPerson().getPersonSource() != null
                                ? p.getPerson().getPersonSource().name()
                                : "VIRTUAL";
                        return new UnbluParticipantData(p.getPerson().getId(), displayName, personSource);
                    })
                    .toList();
        } catch (ApiException e) {
            log.warn("Impossible de récupérer les participants de {} - Status: {} (ignoré)", conversationId, e.getCode());
            return java.util.List.of();
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
     * Recherche les conversations Unblu filtrées par état.
     * Utilise {@code ConversationStateConversationSearchFilter} et parcourt toutes les pages.
     *
     * @param state l'état recherché (INACTIVE, ACTIVE, ENDED, ONBOARDING, OFFBOARDING)
     * @return liste paginée des conversations correspondantes
     * @throws UnbluApiException en cas d'erreur API
     */
    public List<UnbluConversationSummary> searchConversationsByState(String state) {
        try {
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);
            List<ConversationData> all = new java.util.ArrayList<>();
            int offset = 0;
            boolean hasMore = true;
            int pageNum = 0;
            long startTime = System.currentTimeMillis();

            EqualsConversationStateOperator operator = new EqualsConversationStateOperator();
            operator.setType(EConversationStateOperatorType.EQUALS);
            operator.setValue(EConversationState.valueOf(state));

            StateConversationSearchFilter stateFilter = new StateConversationSearchFilter();
            stateFilter.setField(EConversationSearchFilterField.STATE);
            stateFilter.setOperator(operator);

            log.info("Recherche des conversations avec état {} (page size: {})...", state, PAGE_SIZE);
            while (hasMore) {
                long pageStart = System.currentTimeMillis();
                ConversationQuery query = new ConversationQuery();
                query.setOffset(offset);
                query.setLimit(PAGE_SIZE);
                query.addSearchFiltersItem(stateFilter);
                ConversationResult result = conversationsApi.conversationsSearch(query, NO_EXPAND);
                all.addAll(result.getItems());
                hasMore = Boolean.TRUE.equals(result.isHasMoreItems());
                offset = hasMore ? result.getNextOffset() : offset;
                log.info("Page {} chargée en {}ms — {} conversation(s) état={}, hasMore: {}",
                        ++pageNum, System.currentTimeMillis() - pageStart,
                        result.getItems().size(), state, hasMore);
            }

            log.info("Trouvé {} conversation(s) avec état {} en {}ms",
                    all.size(), state, System.currentTimeMillis() - startTime);
            return all.stream()
                    .map(this::toConversationSummary)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new UnbluApiException(400, "Bad Request", "État de conversation invalide : " + state);
        } catch (ApiException e) {
            log.error("Erreur lors de la recherche des conversations par état {} - Status: {}", state, e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error",
                    "Erreur lors de la recherche des conversations : " + e.getMessage());
        }
    }

    /**
     * Génère une URL permettant à un visiteur anonyme de rejoindre une conversation Unblu.
     * Utilise l'InvitationsApi pour créer une invitation avec lien, puis récupère
     * l'URL de type ACCEPT_IN_VISITOR_DESK.
     *
     * @param conversationId identifiant de la conversation cible
     * @return URL d'accès visiteur, ou {@code null} si l'invitation échoue
     */
    public String generateVisitorJoinUrl(String conversationId) {
        try {
            InvitationsApi invitationsApi = new InvitationsApi(apiClient);

            InvitationsInviteAnonymousVisitorToConversationWithLinkBody inviteBody =
                    new InvitationsInviteAnonymousVisitorToConversationWithLinkBody();
            inviteBody.setConversationId(conversationId);

            ConversationInvitationData invitation =
                    invitationsApi.invitationsInviteAnonymousVisitorToConversationWithLink(inviteBody);
            log.info("Invitation créée pour conversationId={}, token={}", conversationId, invitation.getToken());

            AcceptLinkData acceptLink = invitationsApi.invitationsGetAcceptLink(
                    new InvitationsGetAcceptLinkBody().token(invitation.getToken())
            );

            String joinUrl = acceptLink.getLinks().stream()
                    .filter(l -> EConversationLinkType.ACCEPT_IN_VISITOR_DESK.equals(l.getType()))
                    .map(ConversationLink::getUrl)
                    .findFirst()
                    .orElse(null);

            log.info("URL visiteur générée pour conversationId={} : {}", conversationId, joinUrl);
            return joinUrl;
        } catch (ApiException e) {
            log.error("Échec de la génération de l'URL visiteur pour conversationId={} - Status: {}",
                    conversationId, e.getCode(), e);
            return null;
        }
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
