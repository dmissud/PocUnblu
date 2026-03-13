package org.dbs.poc.unblu.application.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.*;

@Component
public class StartDirectConversationRoute extends RouteBuilder {

    private static final String PROP_ORIGINAL_COMMAND = "originalCommand";
    private static final String PROP_CONV_ID = "convId";
    private static final String PROP_VIRTUAL_PERSON = "virtualPerson";
    private static final String PROP_AGENT_PERSON = "agentPerson";

    @Override
    public void configure() {
        from(DIRECT_START_DIRECT_CONVERSATION)
            .routeId("main-orchestrator-start-direct-conversation")
            .log("Démarrage d'une conversation directe - VIRTUAL: ${body.virtualParticipantSourceId}")
            
            .setProperty(PROP_ORIGINAL_COMMAND, body())

            .process(this::prepareVirtualPersonSearch)
            .enrich(DIRECT_UNBLU_SEARCH_PERSONS, this::aggregateVirtualPerson)
            
            .process(this::initContextFromVirtualPerson)
            .enrich(DIRECT_ERP_ADAPTER, this::aggregateCustomerProfile)
            
            .enrich(DIRECT_RULE_ENGINE_ADAPTER, this::aggregateRoutingDecisionAndCheckAuth)
            
            .process(this::prepareAgentPersonSearch)
            .enrich(DIRECT_UNBLU_SEARCH_PERSONS, this::aggregateAgentPerson)
            
            .process(this::prepareDirectConversationRequest)
            .to(DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION)
            
            .process(this::extractConversationId)
            .toD(DIRECT_CONVERSATION_SUMMARY_ADAPTER)
            .process(this::prepareSummaryRequest)
            .to(DIRECT_UNBLU_ADD_SUMMARY)
            .process(this::finalizeConversationInfo);
    }

    private void prepareVirtualPersonSearch(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        exchange.getIn().setBody(new UnbluCamelAdapterPort.PersonSearchRequest(
                cmd.virtualParticipantSourceId(), PersonSource.VIRTUAL));
    }

    private Exchange aggregateVirtualPerson(Exchange oldEx, Exchange newEx) {
        List<PersonInfo> persons = newEx.getIn().getBody(List.class);
        if (persons == null || persons.isEmpty()) throw new IllegalArgumentException("VIRTUAL introuvable");
        oldEx.setProperty(PROP_VIRTUAL_PERSON, persons.getFirst());
        return oldEx;
    }

    private void initContextFromVirtualPerson(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        ConversationContext context = new ConversationContext(
                cmd.virtualParticipantSourceId(),
                "DIRECT_CHANNELS");
        exchange.getIn().setBody(context);
    }

    private Exchange aggregateCustomerProfile(Exchange oldExchange, Exchange newExchange) {
        ConversationContext ctx = oldExchange.getIn().getBody(ConversationContext.class);
        ctx.setCustomerProfile(newExchange.getIn().getBody(org.dbs.poc.unblu.domain.model.CustomerProfile.class));
        return oldExchange;
    }

    private Exchange aggregateRoutingDecisionAndCheckAuth(Exchange oldEx, Exchange newEx) {
        ConversationContext ctx = oldEx.getIn().getBody(ConversationContext.class);
        ChatRoutingDecision decision = newEx.getIn().getBody(ChatRoutingDecision.class);
        ctx.setRoutingDecision(decision);
        if (!ctx.isChatAuthorized()) {
            throw new ChatAccessDeniedException("Accès refusé", decision.routingReason());
        }
        return oldEx;
    }

    private void prepareAgentPersonSearch(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        exchange.getIn().setBody(new UnbluCamelAdapterPort.PersonSearchRequest(
                cmd.agentParticipantSourceId(), PersonSource.USER_DB));
    }

    private Exchange aggregateAgentPerson(Exchange oldEx, Exchange newEx) {
        List<PersonInfo> persons = newEx.getIn().getBody(List.class);
        if (persons == null || persons.isEmpty()) throw new IllegalArgumentException("Agent introuvable");
        oldEx.setProperty(PROP_AGENT_PERSON, persons.getFirst());
        return oldEx;
    }

    private void prepareDirectConversationRequest(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        exchange.getIn().setBody(new UnbluCamelAdapterPort.DirectConversationRequest(
                exchange.getProperty(PROP_VIRTUAL_PERSON, PersonInfo.class),
                exchange.getProperty(PROP_AGENT_PERSON, PersonInfo.class),
                cmd.subject()));
    }

    private void extractConversationId(Exchange exchange) {
        com.unblu.webapi.model.v4.ConversationData data = exchange.getIn().getBody(com.unblu.webapi.model.v4.ConversationData.class);
        exchange.setProperty(PROP_CONV_ID, data.getId());
    }

    private void prepareSummaryRequest(Exchange exchange) {
        String summary = exchange.getIn().getBody(String.class);
        String convId = exchange.getProperty(PROP_CONV_ID, String.class);
        exchange.getIn().setBody(new UnbluCamelAdapterPort.SummaryRequest(convId, summary));
    }

    private void finalizeConversationInfo(Exchange exchange) {
        String convId = exchange.getProperty(PROP_CONV_ID, String.class);
        exchange.getIn().setBody(new UnbluConversationInfo(convId, convId));
    }
}
