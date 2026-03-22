package org.dbs.poc.unblu.exposition.rest;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.dbs.poc.unblu.application.service.OrchestratorEndpoints;
import org.dbs.poc.unblu.domain.model.webhook.WebhookSetupResult;
import org.dbs.poc.unblu.domain.model.webhook.WebhookStatus;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationRequest;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationResponse;
import org.dbs.poc.unblu.exposition.rest.dto.StartDirectConversationRequest;
import org.dbs.poc.unblu.exposition.rest.dto.ConversationHistoryDetailResponse;
import org.dbs.poc.unblu.exposition.rest.dto.ConversationHistoryPageResponse;
import org.dbs.poc.unblu.exposition.rest.dto.SyncConversationsResponse;
import org.dbs.poc.unblu.exposition.rest.mapper.ConversationHistoryQueryMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.ConversationMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.NamedAreaMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.PersonMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.SyncConversationsMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.TeamMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.WebhookMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Camel REST DSL route configuration for Unblu API.
 * Defines all REST endpoints and their mapping to internal Camel routes.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>REST API configuration (Swagger, CORS, JSON binding)</li>
 *   <li>REST endpoint definitions (conversations, persons, teams, webhooks)</li>
 *   <li>Internal route definitions that connect REST endpoints to orchestrators</li>
 * </ul>
 *
 * <p>All business logic and DTO mapping is delegated to dedicated mapper classes.
 */
@Component
public class RestExpositionRoute extends RouteBuilder {

    // Route identifiers
    private static final String ROUTE_REST_START_CONVERSATION = "rest-start-conversation";
    private static final String ROUTE_REST_START_DIRECT_CONVERSATION = "rest-start-direct-conversation";
    private static final String ROUTE_REST_SEARCH_PERSONS = "rest-search-persons";
    private static final String ROUTE_REST_SEARCH_TEAMS = "rest-search-teams";
    private static final String ROUTE_REST_SEARCH_NAMED_AREAS = "rest-search-named-areas";
    private static final String ROUTE_REST_WEBHOOK_SETUP = "rest-webhook-setup";
    private static final String ROUTE_REST_WEBHOOK_STATUS = "rest-webhook-status";
    private static final String ROUTE_REST_WEBHOOK_TEARDOWN = "rest-webhook-teardown";
    private static final String ROUTE_REST_SYNC_CONVERSATIONS = "rest-sync-conversations";
    private static final String ROUTE_REST_LIST_CONVERSATION_HISTORY = "rest-list-conversation-history";
    private static final String ROUTE_REST_GET_CONVERSATION_HISTORY = "rest-get-conversation-history";

    // Internal route URIs
    private static final String DIRECT_REST_START_CONVERSATION = "direct:rest-start-conversation";
    private static final String DIRECT_REST_START_DIRECT_CONVERSATION = "direct:rest-start-direct-conversation";
    private static final String DIRECT_REST_SEARCH_PERSONS = "direct:rest-search-persons";
    private static final String DIRECT_REST_SEARCH_TEAMS = "direct:rest-search-teams";
    private static final String DIRECT_REST_SEARCH_NAMED_AREAS = "direct:rest-search-named-areas";
    private static final String DIRECT_REST_WEBHOOK_SETUP = "direct:rest-webhook-setup";
    private static final String DIRECT_REST_WEBHOOK_STATUS = "direct:rest-webhook-status";
    private static final String DIRECT_REST_WEBHOOK_TEARDOWN = "direct:rest-webhook-teardown";
    private static final String DIRECT_REST_SYNC_CONVERSATIONS = "direct:rest-sync-conversations";
    private static final String DIRECT_REST_LIST_CONVERSATION_HISTORY = "direct:rest-list-conversation-history";
    private static final String DIRECT_REST_GET_CONVERSATION_HISTORY = "direct:rest-get-conversation-history";

    // REST path segments
    private static final String PATH_CONVERSATIONS = "/v1/conversations";
    private static final String PATH_PERSONS = "/v1/persons";
    private static final String PATH_TEAMS = "/v1/teams";
    private static final String PATH_NAMED_AREAS = "/v1/named-areas";
    private static final String PATH_WEBHOOKS = "/v1/webhooks";

    // Swagger configuration
    private static final String API_TITLE = "Unblu Camel Orchestration API";
    private static final String API_VERSION = "1.0.0";
    private static final String API_DESCRIPTION = "API d'orchestration pour l'intégration Unblu utilisant Camel REST DSL";
    private static final String API_CONTACT_NAME = "Equipe Architecture";

    private final ConversationMapper conversationMapper;
    private final PersonMapper personMapper;
    private final TeamMapper teamMapper;
    private final NamedAreaMapper namedAreaMapper;
    private final WebhookMapper webhookMapper;
    private final SyncConversationsMapper syncConversationsMapper;
    private final ConversationHistoryQueryMapper conversationHistoryQueryMapper;

    public RestExpositionRoute(
            ConversationMapper conversationMapper,
            PersonMapper personMapper,
            TeamMapper teamMapper,
            NamedAreaMapper namedAreaMapper,
            WebhookMapper webhookMapper,
            SyncConversationsMapper syncConversationsMapper,
            ConversationHistoryQueryMapper conversationHistoryQueryMapper) {
        this.conversationMapper = conversationMapper;
        this.personMapper = personMapper;
        this.teamMapper = teamMapper;
        this.namedAreaMapper = namedAreaMapper;
        this.webhookMapper = webhookMapper;
        this.syncConversationsMapper = syncConversationsMapper;
        this.conversationHistoryQueryMapper = conversationHistoryQueryMapper;
    }

    @Override
    public void configure() {
        configureRestApi();
        defineRestEndpoints();
        defineInternalRoutes();
    }

    /**
     * Configures REST API global settings (JSON binding, Swagger, CORS).
     */
    private void configureRestApi() {
        restConfiguration()
                .component("servlet")
                .bindingMode(RestBindingMode.json)
                .apiContextPath("/api-doc")
                .contextPath("/api")
                .apiProperty("api.title", API_TITLE)
                .apiProperty("api.version", API_VERSION)
                .apiProperty("api.description", API_DESCRIPTION)
                .apiProperty("api.contact.name", API_CONTACT_NAME)
                .apiProperty("host", "localhost:8081")
                .apiProperty("schemes", "http")
                .apiProperty("cors", "true");
    }

    /**
     * Defines all REST endpoints exposed by the API.
     */
    private void defineRestEndpoints() {
        defineConversationEndpoints();
        definePersonEndpoints();
        defineTeamEndpoints();
        defineNamedAreaEndpoints();
        defineWebhookEndpoints();
    }

    private void defineConversationEndpoints() {
        rest(PATH_CONVERSATIONS)
                .post("/start")
                    .type(StartConversationRequest.class)
                    .outType(StartConversationResponse.class)
                    .to(DIRECT_REST_START_CONVERSATION)
                .post("/direct")
                    .type(StartDirectConversationRequest.class)
                    .outType(StartConversationResponse.class)
                    .to(DIRECT_REST_START_DIRECT_CONVERSATION)
                .post("/sync")
                    .description("Scan toutes les conversations Unblu et synchronise la base de données")
                    .outType(SyncConversationsResponse.class)
                    .to(DIRECT_REST_SYNC_CONVERSATIONS)
                .get("/history")
                    .description("Liste paginée des conversations historisées en base de données")
                    .outType(ConversationHistoryPageResponse.class)
                    .param()
                        .name("page").type(RestParamType.query)
                        .defaultValue("0").description("Numéro de page (0-indexé)")
                    .endParam()
                    .param()
                        .name("size").type(RestParamType.query)
                        .defaultValue("10").description("Nombre d'éléments par page")
                    .endParam()
                    .param()
                        .name("sortField").type(RestParamType.query)
                        .defaultValue("CREATED_AT").description("Champ de tri : CREATED_AT, ENDED_AT, TOPIC")
                    .endParam()
                    .param()
                        .name("sortDir").type(RestParamType.query)
                        .defaultValue("DESC").description("Sens du tri : ASC ou DESC")
                    .endParam()
                    .to(DIRECT_REST_LIST_CONVERSATION_HISTORY)
                .get("/history/{conversationId}")
                    .description("Détail complet d'une conversation avec les événements chronologiques")
                    .outType(ConversationHistoryDetailResponse.class)
                    .param()
                        .name("conversationId").type(RestParamType.path)
                        .description("Identifiant Unblu de la conversation")
                    .endParam()
                    .to(DIRECT_REST_GET_CONVERSATION_HISTORY);
    }

    private void definePersonEndpoints() {
        rest(PATH_PERSONS)
                .get()
                    .outType(List.class)
                    .produces("application/json")
                    .to(DIRECT_REST_SEARCH_PERSONS);
    }

    private void defineTeamEndpoints() {
        rest(PATH_TEAMS)
                .get()
                    .outType(List.class)
                    .produces("application/json")
                    .to(DIRECT_REST_SEARCH_TEAMS);
    }

    private void defineNamedAreaEndpoints() {
        rest(PATH_NAMED_AREAS)
                .get()
                    .outType(List.class)
                    .produces("application/json")
                    .to(DIRECT_REST_SEARCH_NAMED_AREAS)
                .get("/{namedAreaId}/agents")
                    .param()
                        .name("namedAreaId")
                        .type(RestParamType.path)
                        .description("ID of the named area")
                        .required(true)
                    .endParam()
                    .outType(List.class)
                    .produces("application/json")
                    .to("direct:rest-search-agents-by-named-area");
    }

    private void defineWebhookEndpoints() {
        rest(PATH_WEBHOOKS)
                .post("/setup")
                    .description("Setup webhook registration with Unblu")
                    .outType(WebhookSetupResult.class)
                    .to(DIRECT_REST_WEBHOOK_SETUP)
                .get("/status")
                    .description("Get current webhook status")
                    .outType(WebhookStatus.class)
                    .to(DIRECT_REST_WEBHOOK_STATUS)
                .delete("/teardown")
                    .description("Teardown webhook registration")
                    .param()
                        .name("deleteWebhook")
                        .type(RestParamType.query)
                        .defaultValue("false")
                        .description("Whether to delete the webhook from Unblu")
                    .endParam()
                    .to(DIRECT_REST_WEBHOOK_TEARDOWN);

        // Webhook receiver endpoint (called by Unblu)
        rest("/webhooks")
                .post("/unblu")
                    .description("Receive webhook events from Unblu (called by Unblu server)")
                    .consumes("application/json")
                    .type(Map.class)
                    .param()
                        .name("X-Unblu-Signature")
                        .type(RestParamType.header)
                        .description("Webhook signature for verification")
                        .required(false)
                    .endParam()
                    .param()
                        .name("X-Unblu-Event-Type")
                        .type(RestParamType.header)
                        .description("Type of webhook event")
                        .required(false)
                    .endParam()
                    .to("direct:webhook-receiver-internal");
    }

    /**
     * Defines all internal Camel routes that process REST requests.
     * Each route connects a REST endpoint to business logic via mappers and orchestrators.
     */
    private void defineInternalRoutes() {
        defineConversationRoutes();
        definePersonRoutes();
        defineTeamRoutes();
        defineNamedAreaRoutes();
        defineWebhookRoutes();
    }

    private void defineConversationRoutes() {
        from(DIRECT_REST_START_CONVERSATION)
                .routeId(ROUTE_REST_START_CONVERSATION)
                .log("Starting conversation with request: ${body}")
                .process(conversationMapper::mapRequestToCommand)
                .log("Calling orchestrator with command: ${body}")
                .to(OrchestratorEndpoints.DIRECT_START_CONVERSATION)
                .process(conversationMapper::mapContextToResponse)
                .log("Conversation started successfully");

        from(DIRECT_REST_START_DIRECT_CONVERSATION)
                .routeId(ROUTE_REST_START_DIRECT_CONVERSATION)
                .log("Starting direct conversation with request: ${body}")
                .process(conversationMapper::mapDirectRequestToCommand)
                .log("Calling orchestrator with command: ${body}")
                .to(OrchestratorEndpoints.DIRECT_START_DIRECT_CONVERSATION)
                .process(conversationMapper::mapInfoToResponse)
                .log("Direct conversation started successfully");

        from(DIRECT_REST_SYNC_CONVERSATIONS)
                .routeId(ROUTE_REST_SYNC_CONVERSATIONS)
                .log("Démarrage de la synchronisation des conversations")
                .to(OrchestratorEndpoints.DIRECT_SYNC_CONVERSATIONS)
                .process(syncConversationsMapper::mapResultToResponse)
                .log("Synchronisation terminée: ${body}");

        from(DIRECT_REST_LIST_CONVERSATION_HISTORY)
                .routeId(ROUTE_REST_LIST_CONVERSATION_HISTORY)
                .log("Listing conversation history — page: ${header.page}, size: ${header.size}")
                .to(OrchestratorEndpoints.DIRECT_LIST_CONVERSATION_HISTORY)
                .process(conversationHistoryQueryMapper::mapPageToResponse)
                .log("Listed ${body.totalItems} conversation(s)");

        from(DIRECT_REST_GET_CONVERSATION_HISTORY)
                .routeId(ROUTE_REST_GET_CONVERSATION_HISTORY)
                .log("Loading conversation detail: ${header.conversationId}")
                .to(OrchestratorEndpoints.DIRECT_GET_CONVERSATION_HISTORY)
                .process(conversationHistoryQueryMapper::mapDetailToResponse);
    }

    private void definePersonRoutes() {
        from(DIRECT_REST_SEARCH_PERSONS)
                .routeId(ROUTE_REST_SEARCH_PERSONS)
                .log("Searching persons")
                .process(personMapper::searchAndMapPersons)
                .log("Found ${body.size()} persons");
    }

    private void defineTeamRoutes() {
        from(DIRECT_REST_SEARCH_TEAMS)
                .routeId(ROUTE_REST_SEARCH_TEAMS)
                .log("Searching teams")
                .process(teamMapper::searchAndMapTeams)
                .log("Found ${body.size()} teams");
    }

    private void defineNamedAreaRoutes() {
        from(DIRECT_REST_SEARCH_NAMED_AREAS)
                .routeId(ROUTE_REST_SEARCH_NAMED_AREAS)
                .log("Searching named areas")
                .process(namedAreaMapper::searchAndMapNamedAreas)
                .log("Found ${body.size()} named areas");

        from("direct:rest-search-agents-by-named-area")
                .routeId("rest-search-agents-by-named-area")
                .log("Searching agents for named area ${header.namedAreaId}")
                .process(namedAreaMapper::searchAndMapAgentsByNamedArea)
                .log("Found ${body.size()} agents");
    }

    private void defineWebhookRoutes() {
        from(DIRECT_REST_WEBHOOK_SETUP)
                .routeId(ROUTE_REST_WEBHOOK_SETUP)
                .log("Setting up webhook")
                .process(webhookMapper::setupWebhook)
                .log("Webhook setup completed: ${body}");

        from(DIRECT_REST_WEBHOOK_STATUS)
                .routeId(ROUTE_REST_WEBHOOK_STATUS)
                .log("Getting webhook status")
                .process(webhookMapper::getWebhookStatus)
                .log("Webhook status: ${body}");

        from(DIRECT_REST_WEBHOOK_TEARDOWN)
                .routeId(ROUTE_REST_WEBHOOK_TEARDOWN)
                .log("Tearing down webhook")
                .process(webhookMapper::teardownWebhook)
                .log("Webhook teardown completed");
    }
}
