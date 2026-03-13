package org.dbs.poc.unblu.application.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.springframework.stereotype.Component;

import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.*;

@Component
public class StartConversationRoute extends RouteBuilder {

    @Override
    public void configure() {
        from(DIRECT_START_CONVERSATION)
            .routeId("main-orchestrator-start-conversation")
            .log("Démarrage de l'orchestration Camel pour clientId: ${body.clientId}")
            
            .process(this::initConversationContext)
            .enrich(DIRECT_ERP_ADAPTER, this::aggregateCustomerProfile)
            .enrich(DIRECT_RULE_ENGINE_ADAPTER, this::aggregateRoutingDecision)

            .choice()
                .when(simple("${body.chatAuthorized} == false"))
                    .log("Accès refusé par le moteur de règles")
                    .process(this::handleAccessDenied)
                .otherwise()
                    .to(DIRECT_UNBLU_ADAPTER_RESILIENT)
                    .enrich(DIRECT_CONVERSATION_SUMMARY_ADAPTER, (oldExchange, newExchange) -> oldExchange)
                    .to(DIRECT_UNBLU_ADD_SUMMARY_INTERNAL)
            .end();

        from(DIRECT_UNBLU_ADD_SUMMARY_INTERNAL)
            .routeId("main-orchestrator-add-summary-internal")
            .process(this::prepareSummaryRequestFromContext)
            .enrich(DIRECT_CONVERSATION_SUMMARY_ADAPTER, this::aggregateSummary)
            .process(this::prepareFinalSummaryRequest)
            .to(DIRECT_UNBLU_ADD_SUMMARY)
            .process(this::restoreContext);
    }

    private void initConversationContext(Exchange exchange) {
        StartConversationCommand command = exchange.getIn().getBody(StartConversationCommand.class);
        ConversationContext context = new ConversationContext(
                command.clientId(),
                command.origin());
        exchange.getIn().setBody(context);
    }

    private Exchange aggregateCustomerProfile(Exchange oldExchange, Exchange newExchange) {
        ConversationContext ctx = oldExchange.getIn().getBody(ConversationContext.class);
        ctx.setCustomerProfile(newExchange.getIn().getBody(CustomerProfile.class));
        return oldExchange;
    }

    private Exchange aggregateRoutingDecision(Exchange oldExchange, Exchange newExchange) {
        ConversationContext ctx = oldExchange.getIn().getBody(ConversationContext.class);
        ctx.setRoutingDecision(newExchange.getIn().getBody(ChatRoutingDecision.class));
        return oldExchange;
    }

    private void handleAccessDenied(Exchange exchange) {
        ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
        throw new ChatAccessDeniedException("Accès refusé", ctx.getRoutingDecision().routingReason());
    }

    private void prepareSummaryRequestFromContext(Exchange exchange) {
        ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
        exchange.setProperty("convId", ctx.getUnbluConversationId());
        exchange.setProperty("conversationContext", ctx);
    }

    private Exchange aggregateSummary(Exchange oldExchange, Exchange newExchange) {
        String summary = newExchange.getIn().getBody(String.class);
        oldExchange.setProperty("summary", summary);
        return oldExchange;
    }

    private void prepareFinalSummaryRequest(Exchange exchange) {
        String summary = exchange.getProperty("summary", String.class);
        String convId = exchange.getProperty("convId", String.class);
        exchange.getIn().setBody(new org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.SummaryRequest(convId, summary));
    }

    private void restoreContext(Exchange exchange) {
        ConversationContext ctx = exchange.getProperty("conversationContext", ConversationContext.class);
        exchange.getIn().setBody(ctx);
    }
}
