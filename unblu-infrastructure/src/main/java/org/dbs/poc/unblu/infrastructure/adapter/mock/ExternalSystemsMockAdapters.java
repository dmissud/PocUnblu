package org.dbs.poc.unblu.infrastructure.adapter.mock;

import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.springframework.stereotype.Component;

@Component
public class ExternalSystemsMockAdapters extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // ==========================================
        // ADAPTER MOCK : ERP (Récupération Profil)
        // ==========================================
        from("direct:erp-adapter")
            .routeId("mock-erp-adapter")
            .log("Mock ERP appelé pour le client ID: ${body.initialClientId}")
            .process(exchange -> {
                ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
                String clientId = ctx.getInitialClientId();
                
                // Simulation d'une logique métier ERP
                CustomerProfile profile = CustomerProfile.builder()
                        .customerId(clientId)
                        .firstName("Jean")
                        .lastName("Dupont")
                        .isKnown(true)
                        .customerSegment(clientId.startsWith("VIP") ? "VIP" : "STANDARD")
                        .build();

                exchange.getIn().setBody(profile);
            });


        // ==========================================
        // ADAPTER MOCK : Moteur de Règles
        // ==========================================
        from("direct:rule-engine-adapter")
            .routeId("mock-rule-engine-adapter")
            .log("Mock Moteur de Règles appelé pour le segment: ${body.customerProfile.customerSegment}")
            .process(exchange -> {
                ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
                String segment = ctx.getCustomerProfile().getCustomerSegment();
                
                ChatRoutingDecision decision = new ChatRoutingDecision();
                
                if ("BANNED".equalsIgnoreCase(segment)) {
                    decision.setAuthorized(false);
                    decision.setRoutingReason("Client blacklisté - Accès au chat refusé.");
                } else {
                    decision.setAuthorized(true);
                    decision.setRoutingReason("Client éligible.");
                    decision.setUnbluAssignedGroupId("VIP".equalsIgnoreCase(segment) ? "vip_advisors" : "standard_advisors");
                }
                
                exchange.getIn().setBody(decision);
            });
    }
}
