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
        String baseUrl = unbluProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://services8.unblu.com/app/rest/v4";
        }
        // Ensure base URL is absolute and doesn't end with a trailing slash that might cause issues if joined with another slash
        baseUrl = baseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        apiClient.setBasePath(baseUrl);
        apiClient.setUsername(unbluProperties.getUsername());
        apiClient.setPassword(unbluProperties.getPassword());
        return apiClient;
    }
}
