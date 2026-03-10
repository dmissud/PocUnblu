package org.dbs.poc.unblu.infrastructure.adapter.mock;

import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class ExternalSystemsMockAdapters extends RouteBuilder {

    private static final List<String> HELLO_BANK_TEAM_IDS = List.of(
        "cAaYUeKyTZ25_OaA6jUeVA", // Hello bank! Premium
        "xanCWmO_Rluxt0DaUn_11w", // Hello bank! Classic
        "pf50ylVKRRWeMkttXKPwQw", // Hello bank! End2End
        "7iLOw0i9TVCpI8SDAaTXyA"  // Hello bank! Supervision
    );
    private static final Random RANDOM = new Random();

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
                    String teamId = HELLO_BANK_TEAM_IDS.get(RANDOM.nextInt(HELLO_BANK_TEAM_IDS.size()));
                    decision.setUnbluAssignedGroupId(teamId);
                }
                
                exchange.getIn().setBody(decision);
            });
    }
}
