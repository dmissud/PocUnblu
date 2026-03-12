package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.jersey.v4.api.AccountsApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.api.PersonsApi;
import com.unblu.webapi.jersey.v4.api.TeamsApi;
import com.unblu.webapi.jersey.v4.api.WebhookRegistrationsApi;
import com.unblu.webapi.model.v4.*;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.TeamInfo;

import java.util.List;
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
    public List<PersonInfo> searchPersons(String sourceId) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            PersonQuery query = new PersonQuery();

            EqualsPersonSourceOperator sourceOperator = new EqualsPersonSourceOperator();
            sourceOperator.setValue(EPersonSource.VIRTUAL);
            PersonSourcePersonSearchFilter sourceFilter = new PersonSourcePersonSearchFilter();
            sourceFilter.setField(EPersonSearchFilterField.PERSON_SOURCE);
            sourceFilter.setOperator(sourceOperator);
            query.addSearchFiltersItem(sourceFilter);

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
                    .map(p -> PersonInfo.builder()
                            .id(p.getId())
                            .sourceId(p.getSourceId())
                            .displayName(p.getDisplayName())
                            .email(p.getEmail())
                            .build())
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            log.error("Erreur lors de la recherche de personnes dans Unblu - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche de personnes : " + e.getMessage());
        }
    }

    /**
     * Search all teams in Unblu
     */
    public List<TeamInfo> searchTeams() {
        try {
            TeamsApi teamsApi = new TeamsApi(apiClient);
            TeamQuery query = new TeamQuery();

            log.info("Récupération des équipes dans Unblu...");
            TeamResult result = teamsApi.teamsSearch(query, null);
            log.info("Trouvé {} équipe(s)", result.getItems().size());

            return result.getItems().stream()
                    .map(t -> TeamInfo.builder()
                            .id(t.getId())
                            .name(t.getName())
                            .description(t.getDescription())
                            .build())
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            log.error("Erreur lors de la récupération des équipes Unblu - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération des équipes : " + e.getMessage());
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
            data.setVisitorData(virtualPerson.getSourceId());

            ConversationCreationParticipantData virtualParticipant = new ConversationCreationParticipantData();
            virtualParticipant.setParticipationType(EConversationRealParticipationType.CONTEXT_PERSON);
            virtualParticipant.setPersonId(virtualPerson.getId());

            ConversationCreationParticipantData agentParticipant = new ConversationCreationParticipantData();
            agentParticipant.setParticipationType(EConversationRealParticipationType.ASSIGNED_AGENT);
            agentParticipant.setPersonId(agentPerson.getId());

            data.addParticipantsItem(virtualParticipant);
            data.addParticipantsItem(agentParticipant);

            log.info("Création d'une conversation directe dans Unblu - VIRTUAL: {}, Agent: {}", virtualPerson.getId(), agentPerson.getId());
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
}
