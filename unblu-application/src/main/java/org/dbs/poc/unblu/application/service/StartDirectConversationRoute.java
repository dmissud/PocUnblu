package org.dbs.poc.unblu.application.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.domain.model.*;
import org.springframework.stereotype.Component;

import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.*;

/**
 * Route Camel orchestrant la création d'une conversation directe (1-à-1) entre un participant
 * virtuel (visiteur/bot) et un agent Unblu.
 * Enchaîne : recherche de la personne VIRTUAL → enrichissement ERP → moteur de règles
 * → recherche de l'agent → création de la conversation → génération du résumé.
 */
@Component
public class StartDirectConversationRoute extends RouteBuilder {

    private final ConversationWorkflowService workflowService;

    private static final String PROP_ORIGINAL_COMMAND = "originalCommand";
    private static final String PROP_CONV_ID = "convId";
    private static final String PROP_VIRTUAL_PERSON = "virtualPerson";
    private static final String PROP_AGENT_PERSON = "agentPerson";

    public StartDirectConversationRoute(ConversationWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * Définit la route Camel {@code direct:start-direct-conversation} et son pipeline d'orchestration complet.
     */
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

    // --- Camel processors (Exchange wiring only — business logic in ConversationWorkflowService) ---

    /**
     * Recherche la personne VIRTUAL dans Unblu à partir de son {@code sourceId} et la stocke
     * dans les propriétés de l'échange.
     *
     * @param exchange l'échange Camel courant
     */
    private void searchVirtualPerson(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        PersonInfo virtualPerson = workflowService.findVirtualPerson(cmd.virtualParticipantSourceId());
        exchange.setProperty(PROP_VIRTUAL_PERSON, virtualPerson);
        exchange.getIn().setBody(null);
    }

    /**
     * Initialise le {@link ConversationContext} à partir de la personne virtuelle et l'injecte
     * comme corps de l'échange pour les enrichissements suivants.
     *
     * @param exchange l'échange Camel courant
     */
    private void initContextFromVirtualPerson(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        ConversationContext context = new ConversationContext(cmd.virtualParticipantSourceId(), "DIRECT_CHANNELS");
        exchange.getIn().setBody(context);
    }

    /**
     * Agrégateur Camel : injecte le profil client retourné par l'ERP dans le contexte de conversation.
     *
     * @param oldExchange l'échange principal portant le {@link ConversationContext}
     * @param newExchange l'échange de la réponse ERP portant le {@link CustomerProfile}
     * @return l'échange principal enrichi
     */
    private Exchange aggregateCustomerProfile(Exchange oldExchange, Exchange newExchange) {
        ConversationContext ctx = oldExchange.getIn().getBody(ConversationContext.class);
        ctx.setCustomerProfile(newExchange.getIn().getBody(CustomerProfile.class));
        return oldExchange;
    }

    /**
     * Agrégateur Camel : injecte la décision de routage dans le contexte et vérifie l'autorisation d'accès.
     * Lève une {@link org.dbs.poc.unblu.domain.exception.ChatAccessDeniedException} si le chat est refusé.
     *
     * @param oldEx l'échange principal portant le {@link ConversationContext}
     * @param newEx l'échange du moteur de règles portant la {@link ChatRoutingDecision}
     * @return l'échange principal enrichi
     */
    private Exchange aggregateRoutingDecisionAndCheckAuth(Exchange oldEx, Exchange newEx) {
        ConversationContext ctx = oldEx.getIn().getBody(ConversationContext.class);
        ChatRoutingDecision decision = newEx.getIn().getBody(ChatRoutingDecision.class);
        ctx.setRoutingDecision(decision);
        workflowService.validateAuthorization(decision);
        return oldEx;
    }

    /**
     * Recherche la personne AGENT dans Unblu à partir de son {@code sourceId} et la stocke
     * dans les propriétés de l'échange.
     *
     * @param exchange l'échange Camel courant
     */
    private void searchAgentPerson(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        PersonInfo agentPerson = workflowService.findAgentPerson(cmd.agentParticipantSourceId());
        exchange.setProperty(PROP_AGENT_PERSON, agentPerson);
    }

    /**
     * Crée la conversation directe dans Unblu et stocke l'identifiant de la conversation
     * dans les propriétés de l'échange.
     *
     * @param exchange l'échange Camel courant
     */
    private void createDirectConversation(Exchange exchange) {
        StartDirectConversationCommand cmd = exchange.getProperty(PROP_ORIGINAL_COMMAND, StartDirectConversationCommand.class);
        PersonInfo virtualPerson = exchange.getProperty(PROP_VIRTUAL_PERSON, PersonInfo.class);
        PersonInfo agentPerson = exchange.getProperty(PROP_AGENT_PERSON, PersonInfo.class);
        UnbluConversationInfo info = workflowService.createDirectConversation(virtualPerson, agentPerson, cmd.subject());
        exchange.setProperty(PROP_CONV_ID, info.unbluConversationId());
        exchange.getIn().setBody(info);
    }

    /**
     * Génère et attache un résumé à la conversation dont l'identifiant est stocké dans les propriétés.
     *
     * @param exchange l'échange Camel courant
     */
    private void generateAndAddSummary(Exchange exchange) {
        workflowService.addSummary(exchange.getProperty(PROP_CONV_ID, String.class));
    }

    /**
     * Finalise la réponse en construisant un {@link UnbluConversationInfo} à partir de l'identifiant
     * de conversation et l'injecte comme corps final de l'échange.
     *
     * @param exchange l'échange Camel courant
     */
    private void finalizeConversationInfo(Exchange exchange) {
        String convId = exchange.getProperty(PROP_CONV_ID, String.class);
        exchange.getIn().setBody(new UnbluConversationInfo(convId, convId));
    }
}
