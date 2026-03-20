package org.dbs.poc.unblu.application.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.port.secondary.ConversationSummaryPort;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Component;

import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.*;

@Component
public class StartConversationRoute extends RouteBuilder {

    private final UnbluPort unbluPort;
    private final ConversationSummaryPort summaryPort;

    public StartConversationRoute(UnbluPort unbluPort, ConversationSummaryPort summaryPort) {
        this.unbluPort = unbluPort;
        this.summaryPort = summaryPort;
    }

    @Override
    public void configure() {
        from(DIRECT_START_CONVERSATION)
            .routeId("main-orchestrator-start-conversation")
            .log("Démarrage de l'orchestration Camel")

            .process(this::initConversationContext)
            .log("Context initialisé, appel de l'adapter Unblu pour créer la conversation")
            .to(DIRECT_UNBLU_ADAPTER_RESILIENT)
            .log("Conversation créée, ajout du summary")
            .process(this::generateAndAddSummary)
            .log("Summary ajouté à la conversation");
    }

    private void initConversationContext(Exchange exchange) {
        StartConversationCommand command = exchange.getIn().getBody(StartConversationCommand.class);
        ConversationContext context = new ConversationContext(command.clientId(), command.origin());
        ChatRoutingDecision decision = new ChatRoutingDecision(true, command.teamId(), "Team fournie par le front");
        context.setRoutingDecision(decision);
        exchange.getIn().setBody(context);
    }

    private void generateAndAddSummary(Exchange exchange) {
        ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
        String convId = ctx.getUnbluConversationId();
        String summary = summaryPort.generateSummary(convId);
        unbluPort.addSummaryToConversation(convId, summary);
    }
}
