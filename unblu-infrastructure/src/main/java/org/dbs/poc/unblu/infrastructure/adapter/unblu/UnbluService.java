package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.jersey.v4.api.AccountsApi;
import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.api.NamedAreasApi;
import com.unblu.webapi.jersey.v4.api.PersonsApi;
import com.unblu.webapi.jersey.v4.api.TeamsApi;
import com.unblu.webapi.jersey.v4.api.UsersApi;
import com.unblu.webapi.jersey.v4.api.WebhookRegistrationsApi;
import com.unblu.webapi.model.v4.*;
import org.dbs.poc.unblu.domain.model.NamedAreaInfo;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.infrastructure.config.UnbluProperties;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.infrastructure.exception.UnbluApiException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnbluService {

    private final ApiClient apiClient;
    private final UnbluProperties unbluProperties;

    /**
     * Constant to indicate no field expansion is needed in Unblu API calls.
     * When null, only basic data is returned without expanding related entities.
     */
    private static final List<ExpandFields> NO_EXPAND_FIELDS = null;

    /**
     * Get current account from Unblu
     */
    public Account getCurrentAccount() {
        try {
            AccountsApi accountsApi = new AccountsApi(apiClient);

            log.info("Fetching current account from Unblu...");
            Account result = accountsApi.accountsGetCurrentAccount(null);
            log.info("Successfully fetched current account");

            return result;
        } catch (ApiException e) {
            log.error("Error fetching current account from Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour accéder au compte actuel");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération du compte actuel : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching current account from Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la récupération du compte actuel", e);
        }
    }

    /**
     * Example method to list accounts from Unblu
     */
    public AccountResult listAccounts() {
        try {
            AccountsApi accountsApi = new AccountsApi(apiClient);
            AccountQuery query = new AccountQuery();

            log.info("Fetching accounts from Unblu...");
            AccountResult result = accountsApi.accountsSearch(query, null);
            log.info("Successfully fetched {} accounts", result.getItems().size());

            return result;
        } catch (ApiException e) {
            log.error("Error fetching accounts from Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour rechercher les comptes");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche des comptes : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching accounts from Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la recherche des comptes", e);
        }
    }

    /**
     * Create a new conversation
     */
    public ConversationData createConversation(ConversationCreationData conversationData) {
        try {
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);

            log.info("Creating conversation in Unblu...");
            ConversationData result = conversationsApi.conversationsCreate(conversationData, null);
            log.info("Successfully created conversation with ID: {}", result.getId());

            return result;
        } catch (ApiException e) {
            log.error("Error creating conversation in Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour créer une conversation");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création de la conversation : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating conversation in Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la création de la conversation", e);
        }
    }

    /**
     * Search for known persons (VIRTUAL source) in Unblu
     */
    public List<PersonInfo> searchPersons(String sourceId, org.dbs.poc.unblu.domain.model.PersonSource personSource) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            PersonQuery query = new PersonQuery();

            if (personSource != null) {
                EPersonSource ePersonSource = EPersonSource.valueOf(personSource.name());
                EqualsPersonSourceOperator sourceOperator = new EqualsPersonSourceOperator();
                sourceOperator.setValue(ePersonSource);
                PersonSourcePersonSearchFilter sourceFilter = new PersonSourcePersonSearchFilter();
                sourceFilter.setField(EPersonSearchFilterField.PERSON_SOURCE);
                sourceFilter.setOperator(sourceOperator);
                query.addSearchFiltersItem(sourceFilter);
            }

            if (sourceId != null && !sourceId.isBlank()) {
                EqualsIdOperator sourceIdOperator = new EqualsIdOperator();
                sourceIdOperator.setValue(sourceId);
                SourceIdPersonSearchFilter sourceIdFilter = new SourceIdPersonSearchFilter();
                sourceIdFilter.setField(EPersonSearchFilterField.SOURCE_ID);
                sourceIdFilter.setOperator(sourceIdOperator);
                query.addSearchFiltersItem(sourceIdFilter);
            }

            log.info("Recherche de personnes dans Unblu, sourceId: {}", sourceId);
            PersonResult result = personsApi.personsSearch(query, null);
            log.info("Trouvé {} personne(s)", result.getItems().size());

            return result.getItems().stream()
                    .map(this::mapToPersonInfo)
                    .toList();
        } catch (ApiException e) {
            log.error("Erreur lors de la recherche de personnes dans Unblu - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche de personnes : " + e.getMessage());
        }
    }

    private PersonInfo mapToPersonInfo(PersonData personData) {
        return new PersonInfo(
                personData.getId(),
                personData.getSourceId(),
                personData.getDisplayName(),
                personData.getEmail());
    }

    /**
     * Search all teams in Unblu
     */
    public List<TeamInfo> searchTeams() {
        try {
            TeamsApi teamsApi = new TeamsApi(apiClient);
            TeamQuery query = new TeamQuery();

            log.info("Récupération des équipes dans Unblu...");
            TeamResult result = teamsApi.teamsSearch(query, NO_EXPAND_FIELDS);
            log.info("Trouvé {} équipe(s)", result.getItems().size());

            return result.getItems().stream()
                    .map(team -> new TeamInfo(
                            team.getId(),
                            team.getName(),
                            team.getDescription()))
                    .toList();
        } catch (ApiException e) {
            log.error("Erreur lors de la récupération des équipes Unblu - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération des équipes : " + e.getMessage());
        }
    }

    /**
     * Search for named areas
     */
    public List<NamedAreaInfo> searchNamedAreas() {
        try {
            NamedAreasApi namedAreasApi = new NamedAreasApi(apiClient);
            NamedAreaQuery query = new NamedAreaQuery();

            log.info("Récupération des zones nommées dans Unblu...");
            NamedAreaResult result = namedAreasApi.namedAreasSearch(query, NO_EXPAND_FIELDS);
            log.info("Trouvé {} zone(s) nommée(s)", result.getItems().size());

            return result.getItems().stream()
                    .map(namedArea -> new NamedAreaInfo(
                            namedArea.getId(),
                            namedArea.getName(),
                            namedArea.getDescription()))
                    .toList();
        } catch (ApiException e) {
            log.error("Erreur lors de la récupération des zones nommées Unblu - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération des zones nommées : " + e.getMessage());
        }
    }

    /**
     * Search for available agents
     */
    public PersonResult searchAgents(PersonTypedQuery query) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);

            log.info("Searching for agents in Unblu...");
            PersonResult result = personsApi.personsSearchAgents(query, null);
            log.info("Successfully found {} agents", result.getItems().size());

            return result;
        } catch (ApiException e) {
            log.error("Error searching agents in Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour rechercher les agents");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche des agents : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error searching agents in Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la recherche des agents", e);
        }
    }

    /**
     * Search for agents by state (available, away, etc.)
     */
    public AgentPersonStateResult searchAgentsByState(AgentStateQuery query) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);

            log.info("Searching for agents by state in Unblu...");
            AgentPersonStateResult result = personsApi.personsSearchAgentsByState(query);
            log.info("Successfully found {} agents by state", result.getItems().size());

            return result;
        } catch (ApiException e) {
            log.error("Error searching agents by state in Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour rechercher les agents par état");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche des agents par état : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error searching agents by state in Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la recherche des agents par état", e);
        }
    }

    /**
     * Get person by source ID to check if client is known in Unblu
     */
    public PersonData getPersonBySource(EPersonSource personSource, String sourceId) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);

            log.info("Getting person by source from Unblu - Source: {}, SourceId: {}", personSource, sourceId);
            PersonData result = personsApi.personsGetBySource(personSource, sourceId, null);
            log.info("Successfully found person with ID: {}", result.getId());

            return result;
        } catch (ApiException e) {
            log.error("Error getting person by source from Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 404) {
                throw new UnbluApiException(404, "Not Found", "Client non trouvé : aucune personne trouvée avec cette source");
            }
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour rechercher une personne");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche de la personne : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error getting person by source from Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la recherche de la personne", e);
        }
    }

    /**
     * Adds a summary as metadata to an existing Unblu conversation
     */
    /**
     * Creates a bot in Unblu and returns its botPersonId to configure as summary-bot-person-id
     */
    public String createBot(String name, String description) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            BotsApi botsApi = new BotsApi(apiClient);

            // 1. Créer la personne bot (sourceId = identifiant unique externe)
            PersonData botPerson = new PersonData();
            botPerson.setDisplayName(name);
            botPerson.setSourceId("bot-" + name.toLowerCase().replaceAll("[^a-z0-9]", "-"));
            PersonData createdPerson = personsApi.personsCreateOrUpdateBot(botPerson, null);
            String botPersonId = createdPerson.getId();
            log.info("Personne bot créée: id={}", botPersonId);

            // 2. Créer le bot avec l'ID de la personne
            CustomDialogBotData botData = new CustomDialogBotData();
            botData.setName(name);
            botData.setDescription(description);
            botData.setType(EBotType.CUSTOM);
            botData.setBotPersonId(botPersonId);
            botData.setOnboardingFilter(EBotDialogFilter.NONE);
            botData.setOffboardingFilter(EBotDialogFilter.NONE);
            botData.setWebhookStatus(ERegistrationStatus.INACTIVE);
            botData.setWebhookEndpoint("http://localhost/unused");
            botData.setWebhookApiVersion(EWebApiVersion.V4);
            botData.setOutboundTimeoutMillis(5000L);
            botsApi.botsCreate(botData);
            log.info("Bot créé: name={}, botPersonId={}", name, botPersonId);

            return botPersonId;
        } catch (ApiException e) {
            log.error("Erreur lors de la création du bot - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création du bot : " + e.getMessage());
        }
    }

    public void addSummaryToConversation(String conversationId, String summary) {
        try {
            BotsApi botsApi = new BotsApi(apiClient);

            TextPostMessageData messageData = new TextPostMessageData();
            messageData.setType(EPostMessageType.TEXT);
            messageData.setText(summary);
            messageData.setFallbackText(summary);

            String botPersonId = unbluProperties.getSummaryBotPersonId();
            if (botPersonId == null || botPersonId.isBlank()) {
                log.warn("unblu.api.summary-bot-person-id non configuré — résumé non envoyé dans la conversation {}", conversationId);
                return;
            }

            BotPostMessage message = new BotPostMessage();
            message.setConversationId(conversationId);
            message.setSenderPersonId(botPersonId);
            message.setMessageData(messageData);

            // Ajouter le bot comme participant avant d'envoyer le message
            ConversationsApi conversationsApi = new ConversationsApi(apiClient);
            ConversationsAddParticipantBody addParticipantBody = new ConversationsAddParticipantBody();
            addParticipantBody.setPersonId(botPersonId);
            addParticipantBody.setHidden(true);
            conversationsApi.conversationsAddParticipant(conversationId, addParticipantBody, null);

            log.info("Envoi du résumé comme message bot dans la conversation {}", conversationId);
            botsApi.botsSendMessage(message);
            log.info("Résumé envoyé avec succès dans la conversation: {}", conversationId);
        } catch (ApiException e) {
            log.error("Erreur lors de l'envoi du résumé dans la conversation {} - Status: {}", conversationId, e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de l'envoi du résumé : " + e.getMessage());
        }
    }

    /**
     * Create a direct conversation between a VIRTUAL person and a USER_DB agent
     */
    public ConversationData createDirectConversation(
            org.dbs.poc.unblu.domain.model.PersonInfo virtualPerson,
            org.dbs.poc.unblu.domain.model.PersonInfo agentPerson,
            String subject) {
        try {
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

            log.info("Création d'une conversation directe dans Unblu - VIRTUAL: {}, Agent: {}", virtualPerson.id(), agentPerson.id());
            ConversationData result = conversationsApi.conversationsCreate(data, null);
            log.info("Conversation directe créée avec ID: {}", result.getId());

            return result;
        } catch (ApiException e) {
            log.error("Erreur lors de la création de la conversation directe - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création de la conversation directe : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating direct conversation in Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la création de la conversation directe", e);
        }
    }

    /**
     * Search for webhook registrations
     */
    public WebhookRegistrationResult searchWebhooks(WebhookRegistrationQuery query) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);

            log.info("Searching for webhooks in Unblu...");
            WebhookRegistrationResult result = webhookApi.webhookRegistrationsSearch(query);
            log.info("Successfully found {} webhooks", result.getItems().size());

            return result;
        } catch (ApiException e) {
            log.error("Error searching webhooks in Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour rechercher les webhooks");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche des webhooks : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error searching webhooks in Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la recherche des webhooks", e);
        }
    }

    /**
     * Get webhook by ID
     */
    public WebhookRegistration getWebhookById(String registrationId) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);

            log.info("Getting webhook by ID from Unblu - ID: {}", registrationId);
            WebhookRegistration result = webhookApi.webhookRegistrationsRead(registrationId);
            log.info("Successfully found webhook: {}", result.getName());

            return result;
        } catch (ApiException e) {
            log.error("Error getting webhook by ID from Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 404) {
                throw new UnbluApiException(404, "Not Found", "Webhook non trouvé : aucun webhook trouvé avec cet ID");
            }
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour consulter ce webhook");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération du webhook : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error getting webhook by ID from Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la récupération du webhook", e);
        }
    }

    /**
     * Get webhook by name
     */
    public WebhookRegistration getWebhookByName(String name) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);

            log.info("Getting webhook by name from Unblu - Name: {}", name);
            WebhookRegistration result = webhookApi.webhookRegistrationsGetByName(name);
            log.info("Successfully found webhook: {}", result.getName());

            return result;
        } catch (ApiException e) {
            log.error("Error getting webhook by name from Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 404) {
                throw new UnbluApiException(404, "Not Found", "Webhook non trouvé : aucun webhook trouvé avec ce nom");
            }
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour consulter ce webhook");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération du webhook : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error getting webhook by name from Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la récupération du webhook", e);
        }
    }

    /**
     * Create a new webhook registration
     */
    public WebhookRegistration createWebhook(String name, String endpoint, List<String> eventTypes) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);

            log.info("Creating webhook in Unblu - Name: {}, Endpoint: {}, Events: {}", name, endpoint, eventTypes);

            WebhookRegistration webhookData = new WebhookRegistration();
            webhookData.setName(name);
            webhookData.setDescription("Webhook auto-configuré pour le PoC Unblu");
            webhookData.setEndpoint(endpoint);
            webhookData.setApiVersion(EWebApiVersion.V4);
            webhookData.setStatus(ERegistrationStatus.ACTIVE);
            webhookData.setEvents(eventTypes);

            WebhookRegistration result = webhookApi.webhookRegistrationsCreate(webhookData);
            log.info("Successfully created webhook: {} with ID: {}", result.getName(), result.getId());

            return result;
        } catch (ApiException e) {
            log.error("Error creating webhook in Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour créer un webhook");
            }
            if (e.getCode() == 409) {
                throw new UnbluApiException(409, "Conflict", "Un webhook avec ce nom existe déjà");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création du webhook : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating webhook in Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la création du webhook", e);
        }
    }

    /**
     * Update an existing webhook registration
     */
    public WebhookRegistration updateWebhook(String webhookId, String endpoint, List<String> eventTypes) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);

            log.info("Updating webhook in Unblu - ID: {}, New Endpoint: {}", webhookId, endpoint);

            // First, get the existing webhook
            WebhookRegistration existing = webhookApi.webhookRegistrationsRead(webhookId);

            // Update the webhook object
            existing.setEndpoint(endpoint);
            existing.setStatus(ERegistrationStatus.ACTIVE);
            if (eventTypes != null) {
                existing.setEvents(eventTypes);
            }

            WebhookRegistration result = webhookApi.webhookRegistrationsUpdate(existing);
            log.info("Successfully updated webhook: {}", result.getId());

            return result;
        } catch (ApiException e) {
            log.error("Error updating webhook in Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 404) {
                throw new UnbluApiException(404, "Not Found", "Webhook non trouvé : aucun webhook trouvé avec cet ID");
            }
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour mettre à jour ce webhook");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la mise à jour du webhook : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating webhook in Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la mise à jour du webhook", e);
        }
    }

    /**
     * Delete a webhook registration
     */
    public void deleteWebhook(String webhookId) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);

            log.info("Deleting webhook from Unblu - ID: {}", webhookId);
            webhookApi.webhookRegistrationsDelete(webhookId);
            log.info("Successfully deleted webhook: {}", webhookId);
        } catch (ApiException e) {
            log.error("Error deleting webhook from Unblu - Status: {}", e.getCode(), e);
            if (e.getCode() == 404) {
                throw new UnbluApiException(404, "Not Found", "Webhook non trouvé : aucun webhook trouvé avec cet ID");
            }
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Service non autorisé : vous n'avez pas les permissions nécessaires pour supprimer ce webhook");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la suppression du webhook : " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting webhook from Unblu", e);
            throw new RuntimeException("Erreur inattendue lors de la suppression du webhook", e);
        }
    }

    /**
     * Search for agents who have a specific named area in their queue filter configuration
     */
    public List<PersonInfo> searchAgentsByNamedArea(String namedAreaId) {
        try {
            UsersApi usersApi = new UsersApi(apiClient);
            UserQuery query = new UserQuery();

            log.info("Recherche des agents ayant la named area {} dans leur configuration de queue...", namedAreaId);

            // Récupérer tous les utilisateurs avec leur configuration
            List<ExpandFields> expand = List.of(ExpandFields.CONFIGURATION);
            UserResult result = usersApi.usersSearch(query, expand);

            log.info("Trouvé {} utilisateur(s) au total", result.getItems().size());

            // Filtrer les utilisateurs qui ont cette named area dans leur configuration de queue
            List<PersonInfo> agents = result.getItems().stream()
                    .filter(user -> {
                        Map<String, String> config = user.getConfiguration();
                        if (config == null) {
                            return false;
                        }
                        String namedAreasFilter = config.get("com.unblu.queue.ui.defaultFilterNamedAreas");
                        return namedAreasFilter != null && namedAreasFilter.contains(namedAreaId);
                    })
                    .map(user -> new PersonInfo(
                            user.getId(),
                            user.getUsername(),
                            user.getDisplayName(),
                            user.getEmail()))
                    .toList();

            log.info("Trouvé {} agent(s) avec la named area {} dans leur queue", agents.size(), namedAreaId);
            return agents;
        } catch (ApiException e) {
            log.error("Erreur lors de la recherche des agents par named area - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche des agents par named area : " + e.getMessage());
        }
    }
}
