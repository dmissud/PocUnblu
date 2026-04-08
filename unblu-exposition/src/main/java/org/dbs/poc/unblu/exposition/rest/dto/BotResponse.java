package org.dbs.poc.unblu.exposition.rest.dto;

public record BotResponse(
        String id,
        String name,
        String onboardingFilter,
        Integer onboardingOrder,
        String webhookStatus,
        String webhookEndpoint
) {
}
