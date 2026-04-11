package org.dbs.poc.unblu.integration.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.invoker.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.ConversationCreationRequest;
import org.dbs.poc.unblu.integration.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.integration.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.integration.domain.model.UnbluMessageData;
import org.dbs.poc.unblu.integration.domain.model.UnbluParticipantData;
import org.dbs.poc.unblu.integration.domain.port.out.IntegrationUnbluPort;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Bloc 1 — Integration Layer: Unblu SDK adapter.
 * Orchestre les appels vers les clients spécialisés Unblu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationUnbluAdapter implements IntegrationUnbluPort {

    private final UnbluConversationClient conversationClient;
    private final UnbluMessageClient messageClient;
    private final UnbluParticipantClient participantClient;

    // --- createConversation ---

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackCreateConversation")
    @Retry(name = "unblu")
    public UnbluConversationInfo createConversation(ConversationCreationRequest request) {
        try {
            return conversationClient.createConversation(request);
        } catch (ApiException e) {
            log.error("Error creating conversation: {}", e.getMessage());
            throw new RuntimeException("Failed to create conversation: " + e.getMessage(), e);
        }
    }

    private UnbluConversationInfo fallbackCreateConversation(ConversationCreationRequest req, Throwable t) {
        log.warn("Fallback createConversation: {}", t.getMessage());
        return new UnbluConversationInfo("OFFLINE-PENDING", null);
    }

    // --- listAllConversations ---

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackListAll")
    @Retry(name = "unblu")
    public List<UnbluConversationSummary> listAllConversations() {
        try {
            return conversationClient.listAllConversations();
        } catch (ApiException e) {
            log.error("Error listing conversations: {}", e.getMessage());
            throw new RuntimeException("Failed to list conversations", e);
        }
    }

    private List<UnbluConversationSummary> fallbackListAll(Throwable t) {
        log.warn("Fallback listAllConversations: {}", t.getMessage());
        return Collections.emptyList();
    }

    // --- fetchConversationMessages ---

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackMessages")
    public List<UnbluMessageData> fetchConversationMessages(String conversationId) {
        try {
            return messageClient.fetchConversationMessages(conversationId);
        } catch (ApiException e) {
            log.error("Error fetching messages for {}: {}", conversationId, e.getMessage());
            throw new RuntimeException("Failed to fetch messages", e);
        }
    }

    private List<UnbluMessageData> fallbackMessages(String conversationId, Throwable t) {
        log.warn("Fallback fetchConversationMessages({}): {}", conversationId, t.getMessage());
        return Collections.emptyList();
    }

    // --- fetchConversationParticipants ---

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackParticipants")
    public List<UnbluParticipantData> fetchConversationParticipants(String conversationId) {
        try {
            return participantClient.fetchConversationParticipants(conversationId);
        } catch (ApiException e) {
            log.warn("Error fetching participants for {}: {} (ignored)", conversationId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<UnbluParticipantData> fallbackParticipants(String conversationId, Throwable t) {
        log.warn("Fallback fetchConversationParticipants({}): {}", conversationId, t.getMessage());
        return Collections.emptyList();
    }
}
