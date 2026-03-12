package org.dbs.poc.unblu.domain.port.secondary;

public interface ConversationSummaryPort {
    /**
     * Generates a summary for a conversation.
     * @param conversationId the Unblu conversation ID
     * @return a summary text
     */
    String generateSummary(String conversationId);
}
