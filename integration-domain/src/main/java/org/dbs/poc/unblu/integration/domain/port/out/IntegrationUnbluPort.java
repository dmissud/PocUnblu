package org.dbs.poc.unblu.integration.domain.port.out;

import org.dbs.poc.unblu.integration.domain.model.ConversationCreationRequest;
import org.dbs.poc.unblu.integration.domain.model.UnbluConversationInfo;
import org.dbs.poc.unblu.integration.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.integration.domain.model.UnbluMessageData;
import org.dbs.poc.unblu.integration.domain.model.UnbluParticipantData;

import java.util.List;

/**
 * Secondary port towards Unblu API — Bloc 1 Integration.
 * Covers: event processing pipeline + livekit conversation creation.
 */
public interface IntegrationUnbluPort {
    /** Creates a new conversation in Unblu (used by livekit). */
    UnbluConversationInfo createConversation(ConversationCreationRequest request);
    /** Lists all conversations for full sync. */
    List<UnbluConversationSummary> listAllConversations();
    /** Fetches messages for enrich. */
    List<UnbluMessageData> fetchConversationMessages(String conversationId);
    /** Fetches participants for enrich. */
    List<UnbluParticipantData> fetchConversationParticipants(String conversationId);
}
