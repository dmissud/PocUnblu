package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.model.v4.ConversationData;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.model.ConversationOrchestrationState;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Circuit-breaker wrappers for all outbound Unblu API calls.
 *
 * <p>Each resilient route delegates to the corresponding adapter route and
 * defines a fallback strategy appropriate to the operation:
 * <ul>
 *   <li>{@code createConversation} — returns an OFFLINE-PENDING placeholder</li>
 *   <li>{@code createDirectConversation} — returns an OFFLINE-PENDING placeholder</li>
 *   <li>{@code searchPersons} — returns an empty list</li>
 *   <li>{@code addSummary} — silently discards (summary is non-critical)</li>
 * </ul>
 */
@Component
public class UnbluResilientRoute extends RouteBuilder {

    public static final String DIRECT_UNBLU_ADAPTER_RESILIENT = "direct:unblu-adapter-resilient";
    public static final String DIRECT_UNBLU_SEARCH_PERSONS_RESILIENT = "direct:unblu-search-persons-resilient";
    public static final String DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION_RESILIENT = "direct:unblu-create-direct-conversation-resilient";
    public static final String DIRECT_UNBLU_ADD_SUMMARY_RESILIENT = "direct:unblu-add-summary-resilient";

    private static final int TIMEOUT_MS = 3000;

    @Override
    public void configure() throws Exception {

        // --- Create conversation (team) ---
        from(DIRECT_UNBLU_ADAPTER_RESILIENT)
            .routeId("unblu-resilient-create-conversation")
            .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(TIMEOUT_MS).end()
                .to(UnbluCamelAdapter.DIRECT_UNBLU_ADAPTER)
            .onFallback()
                .log("⚠️ Unblu indisponible — fallback createConversation")
                .process(this::fallbackCreateConversation)
            .end();

        // --- Search persons ---
        from(DIRECT_UNBLU_SEARCH_PERSONS_RESILIENT)
            .routeId("unblu-resilient-search-persons")
            .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(TIMEOUT_MS).end()
                .to(UnbluCamelAdapter.DIRECT_UNBLU_SEARCH_PERSONS)
            .onFallback()
                .log("⚠️ Unblu indisponible — fallback searchPersons (liste vide)")
                .process(exchange -> exchange.getIn().setBody(List.of()))
            .end();

        // --- Create direct conversation ---
        from(DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION_RESILIENT)
            .routeId("unblu-resilient-create-direct-conversation")
            .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(TIMEOUT_MS).end()
                .to(UnbluCamelAdapter.DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION)
            .onFallback()
                .log("⚠️ Unblu indisponible — fallback createDirectConversation")
                .process(this::fallbackCreateDirectConversation)
            .end();

        // --- Add summary (non-critical) ---
        from(DIRECT_UNBLU_ADD_SUMMARY_RESILIENT)
            .routeId("unblu-resilient-add-summary")
            .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(TIMEOUT_MS).end()
                .to(UnbluCamelAdapter.DIRECT_UNBLU_ADD_SUMMARY)
            .onFallback()
                .log("⚠️ Unblu indisponible — résumé ignoré (non critique)")
            .end();
    }

    private void fallbackCreateConversation(Exchange exchange) {
        ConversationOrchestrationState state = exchange.getIn().getBody(ConversationOrchestrationState.class);
        state.updateUnbluConversation("OFFLINE-PENDING", "Le service de chat est temporairement indisponible.");
        exchange.getIn().setBody(state);
    }

    private void fallbackCreateDirectConversation(Exchange exchange) {
        ConversationData offline = new ConversationData();
        offline.setId("OFFLINE-PENDING");
        exchange.getIn().setBody(offline);
    }
}
