package org.dbs.poc.unblu.integration.domain.port.out;

/**
 * Secondary port for generating conversation summary (Bloc 1 — used by livekit bot).
 */
public interface ConversationSummaryPort {
    String generateSummary(String conversationId);
}
