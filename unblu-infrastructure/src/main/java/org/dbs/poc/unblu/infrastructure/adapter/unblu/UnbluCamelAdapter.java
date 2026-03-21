package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.DirectConversationRequest;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.PersonSearchRequest;
import org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.SummaryRequest;
import org.springframework.stereotype.Component;

import java.util.List;

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

    private final UnbluPersonService unbluPersonService;
    private final UnbluConversationService unbluConversationService;
    private final UnbluService unbluService;

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
    }

    private void createConversation(org.apache.camel.Exchange exchange) {
        ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);

        ConversationCreationData creationData = new ConversationCreationData();
        creationData.setTopic("Contact depuis " + ctx.getOriginApplication());
        creationData.setVisitorData(ctx.getInitialClientId());
        creationData.setInitialEngagementType(EInitialEngagementType.CHAT_REQUEST);

        com.unblu.webapi.model.v4.ConversationCreationRecipientData recipient = new com.unblu.webapi.model.v4.ConversationCreationRecipientData();
        recipient.setType(com.unblu.webapi.model.v4.EConversationRecipientType.TEAM);
        recipient.setId(ctx.getRoutingDecision().unbluAssignedGroupId());
        creationData.setRecipient(recipient);

        // Récupérer l'ID Unblu de la personne à partir du sourceId
        PersonData person = unbluPersonService.getPersonBySource(EPersonSource.VIRTUAL, ctx.getInitialClientId());

        ConversationCreationParticipantData participant = new ConversationCreationParticipantData();
        participant.setPersonId(person.getId());
        participant.setParticipationType(EConversationRealParticipationType.CONTEXT_PERSON);
        creationData.addParticipantsItem(participant);

        ConversationData response = unbluConversationService.createConversation(creationData);

        ctx.updateUnbluConversation(response.getId(), "https://server.unblu.com/join/" + response.getId());

        exchange.getIn().setBody(ctx);
    }

    private void searchPersons(org.apache.camel.Exchange exchange) {
        PersonSearchRequest req = exchange.getIn().getBody(PersonSearchRequest.class);
        List<PersonInfo> persons = unbluPersonService.searchPersons(req.sourceId(), req.personSource());
        exchange.getIn().setBody(persons);
    }

    private void createDirectConversation(org.apache.camel.Exchange exchange) {
        DirectConversationRequest req = exchange.getIn().getBody(DirectConversationRequest.class);
        ConversationData result = unbluConversationService.createDirectConversation(
                req.virtualPerson(), req.agentPerson(), req.subject());
        exchange.getIn().setBody(result);
    }

    private void addSummaryToConversation(org.apache.camel.Exchange exchange) {
        SummaryRequest req = exchange.getIn().getBody(SummaryRequest.class);
        unbluConversationService.addSummaryToConversation(req.conversationId(), req.summary());
    }

    private void searchTeams(org.apache.camel.Exchange exchange) {
        List<TeamInfo> teams = unbluService.searchTeams();
        exchange.getIn().setBody(teams);
    }

    private void searchNamedAreas(org.apache.camel.Exchange exchange) {
        List<org.dbs.poc.unblu.domain.model.NamedAreaInfo> namedAreas = unbluService.searchNamedAreas();
        exchange.getIn().setBody(namedAreas);
    }

    private void searchAgentsByNamedArea(org.apache.camel.Exchange exchange) {
        String namedAreaId = exchange.getIn().getBody(String.class);
        List<PersonInfo> agents = unbluPersonService.searchAgentsByNamedArea(namedAreaId);
        exchange.getIn().setBody(agents);
    }
}
