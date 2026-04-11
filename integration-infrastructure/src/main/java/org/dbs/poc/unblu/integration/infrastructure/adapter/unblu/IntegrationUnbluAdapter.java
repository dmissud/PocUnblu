package org.dbs.poc.unblu.integration.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.ConversationHistoryApi;
import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.api.InvitationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bloc 1 — Integration Layer: Unblu SDK adapter.
 * Only contains the subset of Unblu API operations needed for event processing and conversation lifecycle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationUnbluAdapter implements IntegrationUnbluPort {

    private static final int PAGE_SIZE = 100;
    private static final int MSG_PAGE_SIZE = 100;

    private final ApiClient integrationUnbluApiClient;

    // --- createConversation ---

    @Override
    @CircuitBreaker(name = "unblu", fallbackMethod = "fallbackCreateConversation")
    @Retry(name = "unblu")
    public UnbluConversationInfo createConversation(ConversationCreationRequest request) {
        try {
            ConversationsApi api = new ConversationsApi(integrationUnbluApiClient);
            var data = new ConversationCreationData();
            data.setTopic(request.topic());
            data.setInitialEngagementType(EInitialEngagementType.CHAT_REQUEST);
            if (request.namedAreaId() != null) {
                var recipient = new ConversationCreationRecipientData();
                recipient.setType(EConversationRecipientType.NAMED_AREA);
                recipient.setId(request.namedAreaId());
                data.setRecipient(recipient);
            }
            if (request.participants() != null) {
                request.participants().forEach(p -> {
                    var participant = new ConversationCreationParticipantData();
                    participant.setPersonId(p.personId());
                    participant.setParticipationType(EConversationRealParticipationType.valueOf(p.participationType()));
                    data.addParticipantsItem(participant);
                });
            }
            var result = api.conversationsCreate(data, null);
            // Get visitor join URL via InvitationsApi
            InvitationsApi invitationsApi = new InvitationsApi(integrationUnbluApiClient);
            InvitationsInviteAnonymousVisitorToConversationWithLinkBody inviteBody = new InvitationsInviteAnonymousVisitorToConversationWithLinkBody();
            inviteBody.setConversationId(result.getId());
            ConversationInvitationData invitation = invitationsApi.invitationsInviteAnonymousVisitorToConversationWithLink(inviteBody);
            
            String joinUrl = null;
            if (invitation != null && invitation.getToken() != null) {
                AcceptLinkData acceptLink = invitationsApi.invitationsGetAcceptLink(
                        new InvitationsGetAcceptLinkBody().token(invitation.getToken()));
                joinUrl = acceptLink != null && acceptLink.getLinks() != null ? 
                    acceptLink.getLinks().stream()
                        .filter(l -> EConversationLinkType.ACCEPT_IN_VISITOR_DESK.equals(l.getType()))
                        .map(ConversationLink::getUrl)
                        .findFirst()
                        .orElse(null) : null;
            }
            return new UnbluConversationInfo(result.getId(), joinUrl);
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
            ConversationsApi api = new ConversationsApi(integrationUnbluApiClient);
            List<ConversationData> all = new ArrayList<>();
            int offset = 0;
            boolean hasMore = true;
            while (hasMore) {
                ConversationQuery query = new ConversationQuery();
                query.setOffset(offset);
                query.setLimit(PAGE_SIZE);
                ConversationResult result = api.conversationsSearch(query, null);
                all.addAll(result.getItems());
                hasMore = Boolean.TRUE.equals(result.isHasMoreItems());
                offset = hasMore ? result.getNextOffset() : offset;
            }
            return all.stream().map(this::toSummary).toList();
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
            ConversationHistoryApi historyApi = new ConversationHistoryApi(integrationUnbluApiClient);
            List<UnbluMessageData> all = new ArrayList<>();
            int offset = 0;
            boolean hasMore = true;
            while (hasMore) {
                MessageExportQuery query = new MessageExportQuery();
                query.setOffset(offset);
                query.setLimit(MSG_PAGE_SIZE);
                MessageExportResult result = historyApi.conversationHistoryExportMessageLog(conversationId, query);
                if (result.getItems() != null) {
                    result.getItems().stream()
                            .filter(m -> m.getText() != null && m.getDeletedForAll() == null)
                            .forEach(m -> all.add(new UnbluMessageData(
                                    m.getId(), m.getText(), m.getSenderPersonId(),
                                    m.getSendTimestamp() != null ? Instant.ofEpochMilli(m.getSendTimestamp()) : Instant.now())));
                }
                hasMore = Boolean.TRUE.equals(result.isHasMoreItems());
                offset = hasMore ? result.getNextOffset() : offset;
            }
            return all;
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
            ConversationHistoryApi historyApi = new ConversationHistoryApi(integrationUnbluApiClient);
            ConversationHistoryData data = historyApi.conversationHistoryRead(conversationId, null);
            if (data.getParticipants() == null) return Collections.emptyList();
            return data.getParticipants().stream()
                    .filter(p -> p.getPerson() != null)
                    .map(p -> {
                        String displayName = p.getPerson().getDisplayName() != null
                                ? p.getPerson().getDisplayName() : p.getPerson().getId();
                        String source = p.getPerson().getPersonSource() != null
                                ? p.getPerson().getPersonSource().name() : "VIRTUAL";
                        return new UnbluParticipantData(p.getPerson().getId(), displayName, source);
                    }).toList();
        } catch (ApiException e) {
            log.warn("Error fetching participants for {}: {} (ignored)", conversationId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<UnbluParticipantData> fallbackParticipants(String conversationId, Throwable t) {
        log.warn("Fallback fetchConversationParticipants({}): {}", conversationId, t.getMessage());
        return Collections.emptyList();
    }

    // --- Helpers ---

    private UnbluConversationSummary toSummary(ConversationData c) {
        return new UnbluConversationSummary(
                c.getId(), c.getTopic(),
                c.getState() != null ? c.getState().name() : "UNKNOWN",
                c.getCreationTimestamp() != null ? Instant.ofEpochMilli(c.getCreationTimestamp()) : Instant.now(),
                c.getEndTimestamp() != null ? Instant.ofEpochMilli(c.getEndTimestamp()) : null);
    }
}
