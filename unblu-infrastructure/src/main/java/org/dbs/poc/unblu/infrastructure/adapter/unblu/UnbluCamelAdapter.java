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

    private final UnbluService unbluService;

    @Override
    public void configure() throws Exception {

        // ==========================================
        // ADAPTER : Unblu (Appel au SDK)
        // ==========================================
        from("direct:unblu-adapter")
            .routeId("unblu-rest-adapter")
            .log("Création de la conversation Unblu pour la file d'attente : ${body.routingDecision.unbluAssignedGroupId}")
            .process(exchange -> {
                ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
                
                // Configuration de la requête de création de conversation
                ConversationCreationData creationData = new ConversationCreationData();
                creationData.setTopic("Contact depuis " + ctx.getOriginApplication());
                creationData.setVisitorData(ctx.getInitialClientId());
                creationData.setInitialEngagementType(EInitialEngagementType.CHAT_REQUEST);
                
                com.unblu.webapi.model.v4.ConversationCreationRecipientData recipient = new com.unblu.webapi.model.v4.ConversationCreationRecipientData();
                recipient.setType(com.unblu.webapi.model.v4.EConversationRecipientType.TEAM);
                recipient.setId(ctx.getRoutingDecision().getUnbluAssignedGroupId());
                creationData.setRecipient(recipient);

                // Ajout du client comme participant CONTEXT_PERSON
                ConversationCreationParticipantData participant = new ConversationCreationParticipantData();
                participant.setPersonId(ctx.getInitialClientId());
                participant.setParticipationType(EConversationRealParticipationType.CONTEXT_PERSON);
                creationData.addParticipantsItem(participant);

                // Appel réel au SDK Unblu via le service
                ConversationData response = unbluService.createConversation(creationData);
                
                ctx.setUnbluConversationId(response.getId());
                
                // Exemple d'URL (à adapter selon les vrais retours ou besoins d'Unblu)
                ctx.setUnbluJoinUrl("https://server.unblu.com/join/" + response.getId());
                
                exchange.getIn().setBody(ctx);
            });

        // ==========================================
        // ADAPTER : Recherche de personnes Unblu
        // ==========================================
        from("direct:unblu-search-persons")
            .routeId("unblu-search-persons")
            .log("Recherche de personnes dans Unblu")
            .process(exchange -> {
                PersonSearchRequest req = exchange.getIn().getBody(PersonSearchRequest.class);
                List<PersonInfo> persons = unbluService.searchPersons(req.sourceId(), req.personSource());
                exchange.getIn().setBody(persons);
            });

        // ==========================================
        // ADAPTER : Conversation directe Unblu
        // ==========================================
        from("direct:unblu-create-direct-conversation")
            .routeId("unblu-create-direct-conversation")
            .log("Création d'une conversation directe dans Unblu")
            .process(exchange -> {
                DirectConversationRequest req = exchange.getIn().getBody(DirectConversationRequest.class);
                ConversationData result = unbluService.createDirectConversation(
                        req.virtualPerson(), req.agentPerson(), req.subject());
                exchange.getIn().setBody(result);
            });

        // ==========================================
        // ADAPTER : Ajout du résumé à une conversation Unblu
        // ==========================================
        from("direct:unblu-add-summary")
            .routeId("unblu-add-summary")
            .log("Ajout du résumé à la conversation Unblu")
            .process(exchange -> {
                SummaryRequest req = exchange.getIn().getBody(SummaryRequest.class);
                unbluService.addSummaryToConversation(req.conversationId(), req.summary());
            });

        // ==========================================
        // ADAPTER : Recherche des équipes Unblu
        // ==========================================
        from("direct:unblu-search-teams")
            .routeId("unblu-search-teams")
            .log("Récupération des équipes Unblu")
            .process(exchange -> {
                List<TeamInfo> teams = unbluService.searchTeams();
                exchange.getIn().setBody(teams);
            });
    }
}
