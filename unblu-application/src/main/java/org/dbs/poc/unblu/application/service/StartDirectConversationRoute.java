package org.dbs.poc.unblu.application.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.PersonSource;
import org.dbs.poc.unblu.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.domain.port.secondary.ConversationSummaryPort;
import org.dbs.poc.unblu.domain.port.secondary.UnbluPort;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.DIRECT_ERP_ADAPTER;
import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.DIRECT_RULE_ENGINE_ADAPTER;
import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.DIRECT_START_DIRECT_CONVERSATION;

@Component
public class StartDirectConversationRoute extends RouteBuilder {

    private final UnbluPort unbluPort;
    private final ConversationSummaryPort summaryPort;

    private static final String PROP_ORIGINAL_COMMAND = "originalCommand";
    private static final String PROP_CONV_ID = "convId";
    private static final String PROP_VIRTUAL_PERSON = "virtualPerson";
    private static final String PROP_AGENT_PERSON = "agentPerson";

    public StartDirectConversationRoute(UnbluPort unbluPort, ConversationSummaryPort summaryPort) {
        this.unbluPort = unbluPort;
        this.summaryPort = summaryPort;
    }

    @Override
    public void configure() {
        from(DIRECT_START_DIRECT_CONVERSATION)
            .routeId("n-orchestrator-start-direct-conversation")
            .log("Démarrage d'une conversation directe")

            .setProperty(PROP_ORIGINAL_COMMAND, body())

            .log("Recherche de la personne VIRTUAL")
            .process(this::searchVirtualPerson)
            .log("Personne VIRTUAL trouvée")

            .process(this::initContextFromVirtualPerson)
            .log("Recherche du profil client dans l'ERP")
            .enrich(DIRECT_ERP_ADAPTER, this::aggregateCustomerProfile)
            .log("Profil client récupéré")

            .log("Appel du moteur de règles pour autorisation")
            .enrich(DIRECT_RULE_ENGINE_ADAPTER, this::aggregateRoutingDecisionAndCheckAuth)
            .log("Décision de routage obtenue")

            .log("Recherche de la personne AGENT")
            .process(this::searchAgentPerson)
            .log("Personne AGENT trouvée")

            .log("Création de la conversation directe")
            .process(this::createDirectConversation)
            .log("Conversation directe créée")

            .log("Génération du summary pour conversation ID: ${exchangeProperty.convId}")
            .process(this::generateAndAddSummary)
            .log("Summary ajouté à la conversation")

            .process(this::finalizeConversationInfo)
            .log("Conversation directe finalisée");
    }

    private void searchVirtualPerson(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        List<PersonInfo> persons = unbluPort.searchPersons(cmd.virtualParticipantSourceId(), PersonSource.VIRTUAL);
        if (persons == null || persons.isEmpty()) throw new IllegalArgumentException("VIRTUAL introuvable");
        exchange.setProperty(PROP_VIRTUAL_PERSON, persons.getFirst());
        exchange.getIn().setBody(null);
    }

    private void initContextFromVirtualPerson(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        ConversationContext context = new ConversationContext(cmd.virtualParticipantSourceId(), "DIRECT_CHANNELS");
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

    private void searchAgentPerson(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        List<PersonInfo> persons = unbluPort.searchPersons(cmd.agentParticipantSourceId(), PersonSource.USER_DB);
        if (persons == null || persons.isEmpty()) throw new IllegalArgumentException("Agent introuvable");
        exchange.setProperty(PROP_AGENT_PERSON, persons.getFirst());
    }

    private void createDirectConversation(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        PersonInfo virtualPerson = exchange.getProperty(PROP_VIRTUAL_PERSON, PersonInfo.class);
        PersonInfo agentPerson = exchange.getProperty(PROP_AGENT_PERSON, PersonInfo.class);
        UnbluConversationInfo info = unbluPort.createDirectConversation(virtualPerson, agentPerson, cmd.subject());
        exchange.setProperty(PROP_CONV_ID, info.unbluConversationId());
        exchange.getIn().setBody(info);
    }

    private void generateAndAddSummary(Exchange exchange) {
        String convId = exchange.getProperty(PROP_CONV_ID, String.class);
        String summary = summaryPort.generateSummary(convId);
        log.info("Summary généré: {}", summary);
        unbluPort.addSummaryToConversation(convId, summary);
    }

    private void finalizeConversationInfo(Exchange exchange) {
        String convId = exchange.getProperty(PROP_CONV_ID, String.class);
        exchange.getIn().setBody(new UnbluConversationInfo(convId, convId));
    }
}
