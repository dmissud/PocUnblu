package org.dbs.poc.unblu.exposition.rest;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.apache.camel.model.rest.RestParamType;
import org.dbs.poc.unblu.application.service.OrchestratorEndpoints;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.model.webhook.WebhookSetupResult;
import org.dbs.poc.unblu.domain.model.webhook.WebhookStatus;
import org.dbs.poc.unblu.exposition.rest.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationRequest;
import org.dbs.poc.unblu.exposition.rest.dto.StartConversationResponse;
import org.dbs.poc.unblu.exposition.rest.dto.StartDirectConversationRequest;
import org.dbs.poc.unblu.exposition.rest.mapper.ConversationMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RestExpositionRoute extends RouteBuilder {

    private final SetupWebhookUseCase setupWebhookUseCase;

    @Value("${mock.rule-engine.default-team-id:cAaYUeKyTZ25_OaA6jUeVA}")
    private String defaultTeamId;

    public RestExpositionRoute(SetupWebhookUseCase setupWebhookUseCase) {
        this.setupWebhookUseCase = setupWebhookUseCase;
    public RestExpositionRoute(
            ConversationMapper conversationMapper,
            PersonMapper personMapper,
    }

    @Override
    public void configure() {
        restConfiguration()
            .component("servlet")
            .bindingMode(RestBindingMode.json)
            .apiContextPath("/api-doc")
            .apiProperty("api.title", "Unblu Camel Orchestration API")
            .apiProperty("api.version", "1.0.0")
            .apiProperty("api.description", "API d'orchestration pour l'intégration Unblu utilisant Camel REST DSL")
            .apiProperty("api.contact.name", "Equipe Architecture")
            .apiProperty("cors", "true");

        // --- Conversations ---
        rest("/v1/conversations")
            .post("/start")
                .type(StartConversationRequest.class)
                .outType(StartConversationResponse.class)
                .to("direct:rest-start-conversation")
            .post("/direct")
                .type(StartDirectConversationRequest.class)
                .outType(StartConversationResponse.class)
                .to("direct:rest-start-direct-conversation");
                .post("/start")
                    .type(StartConversationRequest.class)
                    .outType(StartConversationResponse.class)
                    .to(DIRECT_REST_START_CONVERSATION)
                .post("/direct")
                    .type(StartDirectConversationRequest.class)
                    .outType(StartConversationResponse.class)
                    .to(DIRECT_REST_START_DIRECT_CONVERSATION);

        // --- Persons ---
        rest("/v1/persons")
            .get()
                .outType(List.class)
                .to("direct:rest-search-persons");
        rest(PATH_PERSONS)
                .get()
                    .outType(List.class)
                    .produces("application/json")
                    .to(DIRECT_REST_SEARCH_PERSONS);

        // --- Teams ---
        rest("/v1/teams")
            .get()
                .outType(List.class)
                .to("direct:rest-search-teams");

        // --- Webhooks ---
        rest("/v1/webhooks")
            .post("/setup")
                .outType(WebhookSetupResult.class)
                .to("direct:rest-webhook-setup")
            .get("/status")
                .outType(WebhookStatus.class)
                .to("direct:rest-webhook-status")
            .delete("/teardown")
                .param().name("deleteWebhook").type(org.apache.camel.model.rest.RestParamType.query).defaultValue("false").endParam()
                .to("direct:rest-webhook-teardown");

        // --- Internal routes for mapping and orchestration call ---

    private void defineConversationRoutes() {
        from(DIRECT_REST_START_CONVERSATION)
                .routeId(ROUTE_REST_START_CONVERSATION)
                .process(conversationMapper::mapRequestToCommand)
                .to(OrchestratorEndpoints.DIRECT_START_CONVERSATION)
                .process(conversationMapper::mapContextToResponse);

        from(DIRECT_REST_START_DIRECT_CONVERSATION)
                .routeId(ROUTE_REST_START_DIRECT_CONVERSATION)
                .process(conversationMapper::mapDirectRequestToCommand)
                .to(OrchestratorEndpoints.DIRECT_START_DIRECT_CONVERSATION)
                .process(conversationMapper::mapInfoToResponse);

        from("direct:rest-search-persons")
            .routeId("rest-search-persons")
            .process(this::mapSearchPersonsQuery)
            .to(OrchestratorEndpoints.DIRECT_UNBLU_SEARCH_PERSONS)
            .process(this::mapPersonsToResponse);
    private void definePersonRoutes() {
        from(DIRECT_REST_SEARCH_PERSONS)
                .routeId(ROUTE_REST_SEARCH_PERSONS)
                .process(personMapper::mapHeadersToQuery)
                .to(OrchestratorEndpoints.DIRECT_UNBLU_SEARCH_PERSONS)
                .process(personMapper::mapPersonsToResponse);

        from("direct:rest-search-teams")
            .routeId("rest-search-teams")
            .to(OrchestratorEndpoints.DIRECT_UNBLU_SEARCH_TEAMS)
            .process(this::mapTeamsToResponse);

        from("direct:rest-webhook-setup")
            .routeId("rest-webhook-setup")
            .process(this::setupWebhook);

        from("direct:rest-webhook-status")
            .routeId("rest-webhook-status")
            .process(this::getWebhookStatus);

        from("direct:rest-webhook-teardown")
            .routeId("rest-webhook-teardown")
            .process(this::teardownWebhook);
    }

    private void defineTeamRoutes() {
    }

    protected void mapPersonsToResponse(Exchange exchange) {
        List<?> list = exchange.getIn().getBody(List.class);
        if (list == null) return;
        
        List<PersonResponse> response = list.stream()
                .filter(org.dbs.poc.unblu.domain.model.PersonInfo.class::isInstance)
                .map(org.dbs.poc.unblu.domain.model.PersonInfo.class::cast)
                .map(personInfo -> PersonResponse.builder()
                        .id(personInfo.id())
                        .sourceId(personInfo.sourceId())
                        .displayName(personInfo.displayName())
                        .email(personInfo.email())
                        .build())
                .toList();
        exchange.getIn().setBody(response);
    }

    protected void mapTeamsToResponse(Exchange exchange) {
        List<?> list = exchange.getIn().getBody(List.class);
        if (list == null) return;

        List<TeamResponse> response = list.stream()
                .filter(org.dbs.poc.unblu.domain.model.TeamInfo.class::isInstance)
                .map(org.dbs.poc.unblu.domain.model.TeamInfo.class::cast)
                .map(teamInfo -> TeamResponse.builder()
                        .id(teamInfo.id())
                        .name(teamInfo.name())
                        .description(teamInfo.description())
                        .build())
                .toList();
        exchange.getIn().setBody(response);
    }

    protected void setupWebhook(Exchange exchange) {
        WebhookSetupResult result = setupWebhookUseCase.setupWebhook();
        exchange.getIn().setBody(result);
    }

    protected void getWebhookStatus(Exchange exchange) {
        WebhookStatus status = setupWebhookUseCase.getWebhookStatus();
        exchange.getIn().setBody(status);
    }

    protected void teardownWebhook(Exchange exchange) {
        String deleteWebhookParam = exchange.getIn().getHeader("deleteWebhook", String.class);
        boolean deleteWebhook = Boolean.parseBoolean(deleteWebhookParam);
        setupWebhookUseCase.teardownWebhook(deleteWebhook);
        exchange.getIn().setBody(null);
    }
}
