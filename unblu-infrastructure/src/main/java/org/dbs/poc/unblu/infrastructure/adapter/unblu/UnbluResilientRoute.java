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
    public static final String DIRECT_UNBLU_LIST_CONVERSATIONS_RESILIENT = "direct:unblu-list-conversations-resilient";
    public static final String DIRECT_UNBLU_FETCH_MESSAGES_RESILIENT = "direct:unblu-fetch-messages-resilient";
    public static final String DIRECT_UNBLU_FETCH_PARTICIPANTS_RESILIENT = "direct:unblu-fetch-participants-resilient";
    public static final String DIRECT_UNBLU_SEARCH_CONVERSATIONS_BY_STATE_RESILIENT = "direct:unblu-search-conversations-by-state-resilient";

    private static final int TIMEOUT_MS = 3000;
    private static final int TIMEOUT_MS_BULK = 30000;

    /**
     * Déclare les routes Camel avec circuit breaker Resilience4j pour chaque opération Unblu.
     * Timeout global de {@value #TIMEOUT_MS} ms ; fallback spécifique à chaque opération.
     */
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

        // --- List all conversations ---
        from(DIRECT_UNBLU_LIST_CONVERSATIONS_RESILIENT)
            .routeId("unblu-resilient-list-conversations")
            .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(TIMEOUT_MS_BULK).end()
                .to(UnbluCamelAdapter.DIRECT_UNBLU_LIST_CONVERSATIONS)
            .onFallback()
                .log("⚠️ Unblu indisponible ou timeout (>" + TIMEOUT_MS_BULK + "ms) — fallback listAllConversations (liste vide). Cause: ${exception.message}")
                .process(exchange -> exchange.getIn().setBody(List.of()))
                .end();

        // --- Fetch messages (per conversation) ---
        from(DIRECT_UNBLU_FETCH_MESSAGES_RESILIENT)
                .routeId("unblu-resilient-fetch-messages")
                .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(TIMEOUT_MS_BULK).end()
                .to(UnbluCamelAdapter.DIRECT_UNBLU_FETCH_MESSAGES)
                .onFallback()
                .log("⚠️ Timeout ou erreur lors de la récupération des messages — liste vide. Cause: ${exception.message}")
                .process(exchange -> exchange.getIn().setBody(List.of()))
                .end();

        // --- Fetch participants (per conversation) ---
        from(DIRECT_UNBLU_FETCH_PARTICIPANTS_RESILIENT)
                .routeId("unblu-resilient-fetch-participants")
                .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(TIMEOUT_MS).end()
                .to(UnbluCamelAdapter.DIRECT_UNBLU_FETCH_PARTICIPANTS)
                .onFallback()
                .log("⚠️ Timeout ou erreur lors de la récupération des participants — liste vide. Cause: ${exception.message}")
                .process(exchange -> exchange.getIn().setBody(List.of()))
            .end();

        // --- Search conversations by state ---
        from(DIRECT_UNBLU_SEARCH_CONVERSATIONS_BY_STATE_RESILIENT)
                .routeId("unblu-resilient-search-conversations-by-state")
                .circuitBreaker()
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(TIMEOUT_MS_BULK).end()
                .to(UnbluCamelAdapter.DIRECT_UNBLU_SEARCH_CONVERSATIONS_BY_STATE)
                .onFallback()
                .log("⚠️ Unblu indisponible — fallback searchConversationsByState (liste vide). Cause: ${exception.message}")
                .process(exchange -> exchange.getIn().setBody(List.of()))
                .end();
    }

    /**
     * Fallback de création de conversation : retourne un {@link ConversationOrchestrationState}
     * avec l'identifiant {@code OFFLINE-PENDING} quand Unblu est indisponible.
     *
     * @param exchange l'échange Camel courant
     */
    private void fallbackCreateConversation(Exchange exchange) {
        ConversationOrchestrationState state = exchange.getIn().getBody(ConversationOrchestrationState.class);
        state.updateUnbluConversation("OFFLINE-PENDING", "Le service de chat est temporairement indisponible.");
        exchange.getIn().setBody(state);
    }

    /**
     * Fallback de création de conversation directe : retourne un {@link ConversationData}
     * avec l'identifiant {@code OFFLINE-PENDING} quand Unblu est indisponible.
     *
     * @param exchange l'échange Camel courant
     */
    private void fallbackCreateDirectConversation(Exchange exchange) {
        ConversationData offline = new ConversationData();
        offline.setId("OFFLINE-PENDING");
        exchange.getIn().setBody(offline);
    }
}
