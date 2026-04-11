package org.dbs.poc.unblu.integration.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.ConversationsApi;
import com.unblu.webapi.jersey.v4.api.InvitationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.model.ConversationCreationRequest;
import org.dbs.poc.unblu.integration.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.integration.domain.model.UnbluConversationSummary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service dédié à la gestion des conversations via l'API Unblu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnbluConversationClient {

    private static final int PAGE_SIZE = 100;
    private final ApiClient integrationUnbluApiClient;

    private ConversationsApi conversationsApi() {
        return new ConversationsApi(integrationUnbluApiClient);
    }

    private InvitationsApi invitationsApi() {
        return new InvitationsApi(integrationUnbluApiClient);
    }

    public UnbluConversationInfo createConversation(ConversationCreationRequest request) throws ApiException {
        ConversationCreationData data = buildConversationCreationData(request);
        var result = conversationsApi().conversationsCreate(data, null);

        String joinUrl = getVisitorJoinUrl(result.getId());
        return new UnbluConversationInfo(result.getId(), joinUrl);
    }

    public List<UnbluConversationSummary> listAllConversations() throws ApiException {
        List<ConversationData> allConversations = fetchAllConversationsFromUnblu();
        return allConversations.stream().map(this::toSummary).toList();
    }

    private List<ConversationData> fetchAllConversationsFromUnblu() throws ApiException {
        List<ConversationData> all = new ArrayList<>();
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            ConversationQuery query = new ConversationQuery();
            query.setOffset(offset);
            query.setLimit(PAGE_SIZE);

            ConversationResult result = conversationsApi().conversationsSearch(query, null);
            all.addAll(result.getItems());

            hasMore = Boolean.TRUE.equals(result.isHasMoreItems());
            offset = hasMore ? result.getNextOffset() : offset;
        }
        return all;
    }

    private ConversationCreationData buildConversationCreationData(ConversationCreationRequest request) {
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
        return data;
    }

    private String getVisitorJoinUrl(String conversationId) throws ApiException {
        InvitationsInviteAnonymousVisitorToConversationWithLinkBody inviteBody = new InvitationsInviteAnonymousVisitorToConversationWithLinkBody();
        inviteBody.setConversationId(conversationId);
        ConversationInvitationData invitation = invitationsApi().invitationsInviteAnonymousVisitorToConversationWithLink(inviteBody);

        if (invitation == null || invitation.getToken() == null) {
            return null;
        }

        AcceptLinkData acceptLink = invitationsApi().invitationsGetAcceptLink(
                new InvitationsGetAcceptLinkBody().token(invitation.getToken()));

        if (acceptLink == null || acceptLink.getLinks() == null) {
            return null;
        }

        return acceptLink.getLinks().stream()
                .filter(l -> EConversationLinkType.ACCEPT_IN_VISITOR_DESK.equals(l.getType()))
                .map(ConversationLink::getUrl)
                .findFirst()
                .orElse(null);
    }

    private UnbluConversationSummary toSummary(ConversationData c) {
        return new UnbluConversationSummary(
                c.getId(), c.getTopic(),
                c.getState() != null ? c.getState().name() : "UNKNOWN",
                c.getCreationTimestamp() != null ? Instant.ofEpochMilli(c.getCreationTimestamp()) : Instant.now(),
                c.getEndTimestamp() != null ? Instant.ofEpochMilli(c.getEndTimestamp()) : null);
    }
}
