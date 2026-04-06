package org.dbs.poc.unblu.exposition.rest.config;

import org.dbs.poc.unblu.exposition.rest.mapper.ConversationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for REST API components.
 * Centralizes configuration values and bean definitions.
 */
@Configuration
public class RestApiConfiguration {

    @Value("${mock.rule-engine.default-team-id:cAaYUeKyTZ25_OaA6jUeVA}")
    private String defaultTeamId;

    /**
     * Creates ConversationMapper with injected default team ID.
     */
    @Bean
    public ConversationMapper conversationMapper() {
        return new ConversationMapper(defaultTeamId);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
