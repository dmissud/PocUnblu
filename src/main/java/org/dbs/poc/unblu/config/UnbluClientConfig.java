package org.dbs.poc.unblu.config;

import com.unblu.webapi.jersey.v4.invoker.ApiClient;
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
        apiClient.setUsername(unbluProperties.getUsername());
        apiClient.setPassword(unbluProperties.getPassword());
        return apiClient;
    }
}
