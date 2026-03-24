package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.model.ConversationOrchestrationState;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.DirectConversationRequest;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.PersonSearchRequest;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.SummaryRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Route Camel définissant les endpoints {@code direct:} de l'adaptateur Unblu.
 * Chaque route délègue à un service Unblu spécialisé ({@link UnbluPersonService},
 * {@link UnbluConversationService}, {@link UnbluService}).
 */
@Component
@RequiredArgsConstructor
public class UnbluCamelAdapter extends RouteBuilder {

    public static final String DIRECT_UNBLU_ADAPTER = "direct:unblu-adapter";
    public static final String DIRECT_UNBLU_SEARCH_PERSONS = "direct:unblu-search-persons";
    public static final String DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION = "direct:unblu-create-direct-conversation";
    public static final String DIRECT_UNBLU_ADD_SUMMARY = "direct:unblu-add-summary";
    public static final String DIRECT_UNBLU_SEARCH_TEAMS = "direct:unblu-search-teams";
    public static final String DIRECT_UNBLU_SEARCH_NAMED_AREAS = "direct:unblu-search-named-areas";
    public static final String DIRECT_UNBLU_SEARCH_AGENTS_BY_NAMED_AREA = "direct:unblu-search-agents-by-named-area";
    public static final String DIRECT_UNBLU_LIST_CONVERSATIONS = "direct:unblu-list-conversations";
    public static final String DIRECT_UNBLU_FETCH_MESSAGES = "direct:unblu-fetch-messages";
    public static final String DIRECT_UNBLU_FETCH_PARTICIPANTS = "direct:unblu-fetch-participants";

    private final UnbluPersonService unbluPersonService;
    private final UnbluConversationService unbluConversationService;
    private final UnbluService unbluService;

    /**
     * Déclare toutes les routes Camel de l'adaptateur Unblu :
     * création de conversation, recherche de personnes, conversation directe,
     * ajout de résumé, équipes, zones nommées et agents par zone nommée.
     */
    @Override
    public void configure() throws Exception {

        // ==========================================
        // ADAPTER : Unblu (Appel au SDK)
        // ==========================================
        from(DIRECT_UNBLU_ADAPTER)
            .routeId("unblu-rest-adapter")
            .log("Création de la conversation Unblu")
            .process(this::createConversation);

        // ==========================================
        // ADAPTER : Recherche de personnes Unblu
        // ==========================================
        from(DIRECT_UNBLU_SEARCH_PERSONS)
            .routeId("unblu-search-persons")
            .log("Recherche de personnes dans Unblu")
            .process(this::searchPersons);

        // ==========================================
        // ADAPTER : Conversation directe Unblu
        // ==========================================
        from(DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION)
            .routeId("unblu-create-direct-conversation")
            .log("Création d'une conversation directe dans Unblu")
            .process(this::createDirectConversation);

        // ==========================================
        // ADAPTER : Ajout du résumé à une conversation Unblu
        // ==========================================
        from(DIRECT_UNBLU_ADD_SUMMARY)
            .routeId("unblu-add-summary")
            .log("Ajout du résumé à la conversation Unblu")
            .process(this::addSummaryToConversation);

        // ==========================================
        // ADAPTER : Recherche des équipes Unblu
        // ==========================================
        from(DIRECT_UNBLU_SEARCH_TEAMS)
            .routeId("unblu-search-teams")
            .log("Récupération des équipes Unblu")
            .process(this::searchTeams);

        // ==========================================
        // ADAPTER : Recherche des zones nommées Unblu
        // ==========================================
        from(DIRECT_UNBLU_SEARCH_NAMED_AREAS)
            .routeId("unblu-search-named-areas")
            .log("Récupération des zones nommées Unblu")
            .process(this::searchNamedAreas);

        // ==========================================
        // ADAPTER : Recherche des agents par named area
        // ==========================================
        from(DIRECT_UNBLU_SEARCH_AGENTS_BY_NAMED_AREA)
            .routeId("unblu-search-agents-by-named-area")
            .log("Recherche des agents ayant une named area dans leur queue")
            .process(this::searchAgentsByNamedArea);

        // ==========================================
        // ADAPTER : Liste complète des conversations Unblu
        // ==========================================
        from(DIRECT_UNBLU_LIST_CONVERSATIONS)
            .routeId("unblu-list-conversations")
            .log("Récupération de toutes les conversations Unblu")
            .process(exchange -> exchange.getIn().setBody(unbluConversationService.listAllConversations()));

        from(DIRECT_UNBLU_FETCH_MESSAGES)
                .routeId("unblu-fetch-messages")
                .log("Récupération des messages de la conversation ${body}")
                .process(exchange -> {
                    String conversationId = exchange.getIn().getBody(String.class);
                    exchange.getIn().setBody(unbluConversationService.fetchMessages(conversationId));
                });

        from(DIRECT_UNBLU_FETCH_PARTICIPANTS)
                .routeId("unblu-fetch-participants")
                .log("Récupération des participants de la conversation ${body}")
                .process(exchange -> {
                    String conversationId = exchange.getIn().getBody(String.class);
                    exchange.getIn().setBody(unbluConversationService.fetchParticipants(conversationId));
                });
    }

    /**
     * Processeur Camel : crée une conversation Unblu à partir de l'état d'orchestration et met à jour
     * l'identifiant et l'URL de la conversation dans cet état.
     *
     * @param exchange l'échange Camel portant un {@link ConversationOrchestrationState}
     */
    private void createConversation(org.apache.camel.Exchange exchange) {
        ConversationOrchestrationState state = exchange.getIn().getBody(ConversationOrchestrationState.class);
        ConversationContext ctx = state.context();

        ConversationCreationData creationData = new ConversationCreationData();
        creationData.setTopic("Contact depuis " + ctx.originApplication());
        creationData.setVisitorData(ctx.initialClientId());
        creationData.setInitialEngagementType(EInitialEngagementType.CHAT_REQUEST);

        com.unblu.webapi.model.v4.ConversationCreationRecipientData recipient = new com.unblu.webapi.model.v4.ConversationCreationRecipientData();
        recipient.setType(com.unblu.webapi.model.v4.EConversationRecipientType.TEAM);
        recipient.setId(ctx.routingDecision().unbluAssignedGroupId());
        creationData.setRecipient(recipient);

        PersonData person = unbluPersonService.getPersonBySource(EPersonSource.VIRTUAL, ctx.initialClientId());

        ConversationCreationParticipantData participant = new ConversationCreationParticipantData();
        participant.setPersonId(person.getId());
        participant.setParticipationType(EConversationRealParticipationType.CONTEXT_PERSON);
        creationData.addParticipantsItem(participant);

        ConversationData response = unbluConversationService.createConversation(creationData);

        state.updateUnbluConversation(response.getId(), "https://server.unblu.com/join/" + response.getId());

        exchange.getIn().setBody(state);
    }

    /**
     * Processeur Camel : recherche des personnes Unblu à partir d'une {@link UnbluCamelAdapterPort.PersonSearchRequest}.
     *
     * @param exchange l'échange Camel portant la requête de recherche
     */
    private void searchPersons(org.apache.camel.Exchange exchange) {
        PersonSearchRequest req = exchange.getIn().getBody(PersonSearchRequest.class);
        List<PersonInfo> persons = unbluPersonService.searchPersons(req.sourceId(), req.personSource());
        exchange.getIn().setBody(persons);
    }

    /**
     * Processeur Camel : crée une conversation directe Unblu à partir d'une
     * {@link UnbluCamelAdapterPort.DirectConversationRequest}.
     *
     * @param exchange l'échange Camel portant la requête de conversation directe
     */
    private void createDirectConversation(org.apache.camel.Exchange exchange) {
        DirectConversationRequest req = exchange.getIn().getBody(DirectConversationRequest.class);
        ConversationData result = unbluConversationService.createDirectConversation(
                req.virtualPerson(), req.agentPerson(), req.subject());
        exchange.getIn().setBody(result);
    }

    /**
     * Processeur Camel : ajoute un résumé textuel à une conversation Unblu via une
     * {@link UnbluCamelAdapterPort.SummaryRequest}.
     *
     * @param exchange l'échange Camel portant la requête de résumé
     */
    private void addSummaryToConversation(org.apache.camel.Exchange exchange) {
        SummaryRequest req = exchange.getIn().getBody(SummaryRequest.class);
        unbluConversationService.addSummaryToConversation(req.conversationId(), req.summary());
    }

    /**
     * Processeur Camel : récupère toutes les équipes Unblu et les place dans le corps de l'échange.
     *
     * @param exchange l'échange Camel courant
     */
    private void searchTeams(org.apache.camel.Exchange exchange) {
        List<TeamInfo> teams = unbluService.searchTeams();
        exchange.getIn().setBody(teams);
    }

    /**
     * Processeur Camel : récupère toutes les zones nommées Unblu et les place dans le corps de l'échange.
     *
     * @param exchange l'échange Camel courant
     */
    private void searchNamedAreas(org.apache.camel.Exchange exchange) {
        List<org.dbs.poc.unblu.domain.model.NamedAreaInfo> namedAreas = unbluService.searchNamedAreas();
        exchange.getIn().setBody(namedAreas);
    }

    /**
     * Processeur Camel : recherche les agents Unblu associés à une zone nommée.
     * L'identifiant de la zone est lu depuis le corps de l'échange.
     *
     * @param exchange l'échange Camel portant l'identifiant de la zone nommée
     */
    private void searchAgentsByNamedArea(org.apache.camel.Exchange exchange) {
        String namedAreaId = exchange.getIn().getBody(String.class);
        List<PersonInfo> agents = unbluPersonService.searchAgentsByNamedArea(namedAreaId);
        exchange.getIn().setBody(agents);
    }
}
