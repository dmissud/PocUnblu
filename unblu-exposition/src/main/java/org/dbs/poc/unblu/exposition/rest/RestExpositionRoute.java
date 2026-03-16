package org.dbs.poc.unblu.exposition.rest;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.dbs.poc.unblu.application.port.in.SearchPersonsQuery;
import org.dbs.poc.unblu.application.port.in.SetupWebhookUseCase;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.application.service.OrchestratorEndpoints;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.model.webhook.WebhookSetupResult;
import org.dbs.poc.unblu.domain.model.webhook.WebhookStatus;
import org.dbs.poc.unblu.exposition.rest.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RestExpositionRoute extends RouteBuilder {

    private final SetupWebhookUseCase setupWebhookUseCase;

    @Value("${mock.rule-engine.default-team-id:cAaYUeKyTZ25_OaA6jUeVA}")
    private String defaultTeamId;

    public RestExpositionRoute(SetupWebhookUseCase setupWebhookUseCase) {
        this.setupWebhookUseCase = setupWebhookUseCase;
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

        // --- Persons ---
        rest("/v1/persons")
            .get()
                .outType(List.class)
                .to("direct:rest-search-persons");

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

        from("direct:rest-start-conversation")
            .routeId("rest-start-conversation")
            .process(this::mapStartConversationRequestToCommand)
            .to(OrchestratorEndpoints.DIRECT_START_CONVERSATION)
            .process(this::mapContextToResponse);

        from("direct:rest-start-direct-conversation")
            .routeId("rest-start-direct-conversation")
            .process(this::mapStartDirectConversationRequestToCommand)
            .to(OrchestratorEndpoints.DIRECT_START_DIRECT_CONVERSATION)
            .process(this::mapUnbluInfoToResponse);

        from("direct:rest-search-persons")
            .routeId("rest-search-persons")
            .process(this::mapSearchPersonsQuery)
            .to(OrchestratorEndpoints.DIRECT_UNBLU_SEARCH_PERSONS)
            .process(this::mapPersonsToResponse);

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

    protected void mapStartConversationRequestToCommand(Exchange exchange) {
        StartConversationRequest request = exchange.getIn().getBody(StartConversationRequest.class);
        exchange.getIn().setBody(new StartConversationCommand(
                request.getClientId(),
                request.getSubject(),
                request.getOrigin(),
                defaultTeamId));
    }

    protected void mapContextToResponse(Exchange exchange) {
        ConversationContext context = exchange.getIn().getBody(ConversationContext.class);
        exchange.getIn().setBody(StartConversationResponse.builder()
                .unbluConversationId(context.getUnbluConversationId())
                .unbluJoinUrl(context.getUnbluJoinUrl())
                .status("CREATED")
                .message("Conversation successfully created.")
                .build());
    }

    protected void mapStartDirectConversationRequestToCommand(Exchange exchange) {
        StartDirectConversationRequest request = exchange.getIn().getBody(StartDirectConversationRequest.class);
        exchange.getIn().setBody(new StartDirectConversationCommand(
                request.getVirtualParticipantSourceId(),
                request.getAgentParticipantSourceId(),
                request.getSubject()));
    }

    protected void mapUnbluInfoToResponse(Exchange exchange) {
        UnbluConversationInfo info = exchange.getIn().getBody(UnbluConversationInfo.class);
        exchange.getIn().setBody(StartConversationResponse.builder()
                .unbluConversationId(info.unbluConversationId())
                .unbluJoinUrl(info.unbluJoinUrl())
                .status("CREATED")
                .message("Conversation directe créée avec succès.")
                .build());
    }

    protected void mapSearchPersonsQuery(Exchange exchange) {
        String sourceId = exchange.getIn().getHeader("sourceId", String.class);
        String personSourceStr = exchange.getIn().getHeader("personSource", String.class);
        PersonSource personSource = personSourceStr != null ? PersonSource.valueOf(personSourceStr) : null;
        
        exchange.getIn().setBody(new SearchPersonsQuery(sourceId, personSource));
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
