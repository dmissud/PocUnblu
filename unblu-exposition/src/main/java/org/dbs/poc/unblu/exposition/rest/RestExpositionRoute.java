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
import org.dbs.poc.unblu.exposition.rest.mapper.ConversationMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.PersonMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.TeamMapper;
import org.dbs.poc.unblu.exposition.rest.mapper.WebhookMapper;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private static final String ROUTE_REST_WEBHOOK_SETUP = "rest-webhook-setup";
    private static final String ROUTE_REST_WEBHOOK_STATUS = "rest-webhook-status";
    private static final String ROUTE_REST_WEBHOOK_TEARDOWN = "rest-webhook-teardown";

    // Internal route URIs
    private static final String DIRECT_REST_START_CONVERSATION = "direct:rest-start-conversation";
    private static final String DIRECT_REST_START_DIRECT_CONVERSATION = "direct:rest-start-direct-conversation";
    private static final String DIRECT_REST_SEARCH_PERSONS = "direct:rest-search-persons";
    private static final String DIRECT_REST_SEARCH_TEAMS = "direct:rest-search-teams";
    private static final String DIRECT_REST_WEBHOOK_SETUP = "direct:rest-webhook-setup";
    private static final String DIRECT_REST_WEBHOOK_STATUS = "direct:rest-webhook-status";
    private static final String DIRECT_REST_WEBHOOK_TEARDOWN = "direct:rest-webhook-teardown";

    // REST path segments
    private static final String PATH_CONVERSATIONS = "/v1/conversations";
    private static final String PATH_PERSONS = "/v1/persons";
    private static final String PATH_TEAMS = "/v1/teams";
    private static final String PATH_WEBHOOKS = "/v1/webhooks";

    // Swagger configuration
    private static final String API_TITLE = "Unblu Camel Orchestration API";
    private static final String API_VERSION = "1.0.0";
    private static final String API_DESCRIPTION = "API d'orchestration pour l'intégration Unblu utilisant Camel REST DSL";
    private static final String API_CONTACT_NAME = "Equipe Architecture";

    private final ConversationMapper conversationMapper;
    private final PersonMapper personMapper;
    private final TeamMapper teamMapper;
    private final WebhookMapper webhookMapper;

    public RestExpositionRoute(
            ConversationMapper conversationMapper,
            PersonMapper personMapper,
            TeamMapper teamMapper,
            WebhookMapper webhookMapper) {
        this.conversationMapper = conversationMapper;
        this.personMapper = personMapper;
        this.teamMapper = teamMapper;
        this.webhookMapper = webhookMapper;
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
                .apiProperty("api.title", API_TITLE)
                .apiProperty("api.version", API_VERSION)
                .apiProperty("api.description", API_DESCRIPTION)
                .apiProperty("api.contact.name", API_CONTACT_NAME)
                .apiProperty("cors", "true");
    }

    /**
     * Defines all REST endpoints exposed by the API.
     */
    private void defineRestEndpoints() {
        defineConversationEndpoints();
        definePersonEndpoints();
        defineTeamEndpoints();
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
                    .to(DIRECT_REST_START_DIRECT_CONVERSATION);
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

    private void defineWebhookEndpoints() {
        rest(PATH_WEBHOOKS)
                .post("/setup")
                    .outType(WebhookSetupResult.class)
                    .to(DIRECT_REST_WEBHOOK_SETUP)
                .get("/status")
                    .outType(WebhookStatus.class)
                    .to(DIRECT_REST_WEBHOOK_STATUS)
                .delete("/teardown")
                    .param()
                        .name("deleteWebhook")
                        .type(RestParamType.query)
                        .defaultValue("false")
                        .description("Whether to delete the webhook from Unblu")
                    .endParam()
                    .to(DIRECT_REST_WEBHOOK_TEARDOWN);
    }

    /**
     * Defines all internal Camel routes that process REST requests.
     * Each route connects a REST endpoint to business logic via mappers and orchestrators.
     */
    private void defineInternalRoutes() {
        defineConversationRoutes();
        definePersonRoutes();
        defineTeamRoutes();
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
