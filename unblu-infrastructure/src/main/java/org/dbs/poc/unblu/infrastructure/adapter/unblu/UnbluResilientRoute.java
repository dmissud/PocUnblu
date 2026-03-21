package org.dbs.poc.unblu.infrastructure.adapter.unblu;


import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.model.ConversationOrchestrationState;
import org.springframework.stereotype.Component;

@Component
public class UnbluResilientRoute extends RouteBuilder {

    public static final String DIRECT_UNBLU_ADAPTER_RESILIENT = "direct:unblu-adapter-resilient";

    @Override
    public void configure() throws Exception {
        // We wrap the base unblu rest adapter with a circuit breaker
        from(DIRECT_UNBLU_ADAPTER_RESILIENT)
            .routeId("unblu-resilient-wrapper")
            .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(3000).end()
                .to(UnbluCamelAdapter.DIRECT_UNBLU_ADAPTER)
            .onFallback()
                .log("⚠️ L'API Unblu est injoignable ou a expiré. Déclenchement du Fallback.")
                .process(this::handleFallback)
            .end();
    }

    private void handleFallback(org.apache.camel.Exchange exchange) {
        ConversationOrchestrationState state = exchange.getIn().getBody(ConversationOrchestrationState.class);
        state.updateUnbluConversation("OFFLINE-PENDING", "Le service de chat est temporairement indisponible.");
        exchange.getIn().setBody(state);
    }
}
