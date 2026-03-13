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

    private final UnbluService unbluService;

    @Override
    public void configure() throws Exception {

        // ==========================================
        // ADAPTER : Unblu (Appel au SDK)
        // ==========================================
        from(DIRECT_UNBLU_ADAPTER)
            .routeId("unblu-rest-adapter")
            .log("Création de la conversation Unblu pour la file d'attente : ${body.routingDecision.unbluAssignedGroupId}")
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

        ConversationCreationParticipantData participant = new ConversationCreationParticipantData();
        participant.setPersonId(ctx.getInitialClientId());
        participant.setParticipationType(EConversationRealParticipationType.CONTEXT_PERSON);
        creationData.addParticipantsItem(participant);

        ConversationData response = unbluService.createConversation(creationData);

        ctx.updateUnbluConversation(response.getId(), "https://server.unblu.com/join/" + response.getId());

        exchange.getIn().setBody(ctx);
    }

    private void searchPersons(org.apache.camel.Exchange exchange) {
        PersonSearchRequest req = exchange.getIn().getBody(PersonSearchRequest.class);
        List<PersonInfo> persons = unbluService.searchPersons(req.sourceId(), req.personSource());
        exchange.getIn().setBody(persons);
    }

    private void createDirectConversation(org.apache.camel.Exchange exchange) {
        DirectConversationRequest req = exchange.getIn().getBody(DirectConversationRequest.class);
        ConversationData result = unbluService.createDirectConversation(
                req.virtualPerson(), req.agentPerson(), req.subject());
        exchange.getIn().setBody(result);
    }

    private void addSummaryToConversation(org.apache.camel.Exchange exchange) {
        SummaryRequest req = exchange.getIn().getBody(SummaryRequest.class);
        unbluService.addSummaryToConversation(req.conversationId(), req.summary());
    }

    private void searchTeams(org.apache.camel.Exchange exchange) {
        List<TeamInfo> teams = unbluService.searchTeams();
        exchange.getIn().setBody(teams);
    }
}
