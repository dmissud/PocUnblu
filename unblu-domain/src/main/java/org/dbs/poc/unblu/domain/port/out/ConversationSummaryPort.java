package org.dbs.poc.unblu.domain.port.out;

/**
 * Port secondaire vers le service de génération de résumé de conversation.
 * Le résumé généré est ensuite posté dans la conversation Unblu via le bot.
 */
public interface ConversationSummaryPort {
    /**
     * Generates a summary for a conversation.
     * @param conversationId the Unblu conversation ID
     * @return a summary text
     */
    String generateSummary(String conversationId);
}
