package org.dbs.poc.unblu.config;

import com.unblu.webapi.jersey.v3.ApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class UnbluClientConfig {

    private final UnbluProperties unbluProperties;

    @Bean
    public ApiClient unbluApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(unbluProperties.getBaseUrl());
        apiClient.setApiKey(unbluProperties.getApiKey());
        return apiClient;
    }
}
