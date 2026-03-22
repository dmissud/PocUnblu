package org.dbs.poc.unblu.infrastructure.adapter.mock;

import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Adaptateur mock Camel simulant les systèmes externes ERP et moteur de règles.
 * Activé par défaut via {@code mock.rule-engine.enabled=true} (ou en l'absence de la propriété).
 * Simule trois segments clients : {@code VIP} (préfixe "VIP"), {@code BANNED}, et {@code STANDARD}.
 */
@Component
@ConditionalOnProperty(name = "mock.rule-engine.enabled", havingValue = "true", matchIfMissing = true)
public class ExternalSystemsMockAdapters extends RouteBuilder {

    public static final String DIRECT_ERP_ADAPTER = "direct:erp-adapter";
    public static final String DIRECT_RULE_ENGINE_ADAPTER = "direct:rule-engine-adapter";

    private static final List<String> HELLO_BANK_TEAM_IDS = List.of(
        "cAaYUeKyTZ25_OaA6jUeVA", // Hello bank! Premium
        "xanCWmO_Rluxt0DaUn_11w", // Hello bank! Classic
        "pf50ylVKRRWeMkttXKPwQw", // Hello bank! End2End
        "7iLOw0i9TVCpI8SDAaTXyA"  // Hello bank! Supervision
    );
    private static final Random RANDOM = new Random();

    @Value("${mock.rule-engine.default-team-id:cAaYUeKyTZ25_OaA6jUeVA}")
    private String defaultTeamId;

    /**
     * Déclare les routes Camel mock : {@code direct:erp-adapter} et {@code direct:rule-engine-adapter}.
     */
    @Override
    public void configure() throws Exception {

        // ==========================================
        // ADAPTER MOCK : ERP (Récupération Profil)
        // ==========================================
        from(DIRECT_ERP_ADAPTER)
            .routeId("mock-erp-adapter")
            .log("Mock ERP appelé")
            .process(this::mockErpLogic);


        // ==========================================
        // ADAPTER MOCK : Moteur de Règles
        // ==========================================
        from(DIRECT_RULE_ENGINE_ADAPTER)
            .routeId("mock-rule-engine-adapter")
            .log("Mock Moteur de Règles appelé")
            .process(this::mockRuleEngineLogic);
    }

    /**
     * Logique mock ERP : retourne un {@link CustomerProfile} fictif.
     * Le segment est {@code VIP} si le clientId commence par "VIP", sinon {@code STANDARD}.
     *
     * @param exchange l'échange Camel portant un {@link ConversationContext}
     */
    private void mockErpLogic(org.apache.camel.Exchange exchange) {
        ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
        String clientId = ctx.initialClientId();
        
        // Simulation d'une logique métier ERP
        CustomerProfile profile = new CustomerProfile(
                clientId,
                "Jean",
                "Dupont",
                clientId.startsWith("VIP") ? "VIP" : "STANDARD",
                true);

        exchange.getIn().setBody(profile);
    }

    /**
     * Logique mock du moteur de règles : refuse l'accès aux clients bannis,
     * sinon retourne une décision positive avec l'équipe configurée ({@code mock.rule-engine.default-team-id}).
     *
     * @param exchange l'échange Camel portant un {@link ConversationContext} enrichi du profil client
     */
    private void mockRuleEngineLogic(org.apache.camel.Exchange exchange) {
        ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);

        ChatRoutingDecision decision;

        if (ctx.customerProfile().isBanned()) {
            decision = new ChatRoutingDecision(false, null, "Client blacklisté - Accès au chat refusé.");
        } else {
            // Utilise le teamId configuré au lieu d'un choix aléatoire
            decision = new ChatRoutingDecision(true, defaultTeamId, "Client éligible.");
        }

        exchange.getIn().setBody(decision);
    }
}
