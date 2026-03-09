package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.model.v4.ConversationCreationData;
import com.unblu.webapi.model.v4.ConversationData;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.springframework.stereotype.Component;

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
                
                com.unblu.webapi.model.v4.ConversationCreationRecipientData recipient = new com.unblu.webapi.model.v4.ConversationCreationRecipientData();
                recipient.setType(com.unblu.webapi.model.v4.EConversationRecipientType.TEAM);
                recipient.setId(ctx.getRoutingDecision().getUnbluAssignedGroupId());
                creationData.setRecipient(recipient);
                
                // Appel réel au SDK Unblu via le service
                ConversationData response = unbluService.createConversation(creationData);
                
                ctx.setUnbluConversationId(response.getId());
                
                // Exemple d'URL (à adapter selon les vrais retours ou besoins d'Unblu)
                ctx.setUnbluJoinUrl("https://server.unblu.com/join/" + response.getId());
                
                exchange.getIn().setBody(ctx);
            });
    }
}
