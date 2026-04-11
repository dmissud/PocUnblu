package org.dbs.poc.unblu.integration.infrastructure.config;

import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class UnbluClientConfig {

    private final UnbluProperties unbluProperties;

    @Bean
    public ApiClient integrationUnbluApiClient() {
        ApiClient apiClient = new ApiClient();
        String baseUrl = unbluProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://services8.unblu.com/app/rest/v4";
        }
        baseUrl = baseUrl.trim();
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        apiClient.setBasePath(baseUrl);
        apiClient.setUsername(unbluProperties.getUsername());
        apiClient.setPassword(unbluProperties.getPassword());
        configureProxy(apiClient);
        return apiClient;
    }

    private void configureProxy(ApiClient apiClient) {
        UnbluProperties.ProxyProperties proxy = unbluProperties.getProxy();
        if (proxy != null && proxy.getHost() != null && !proxy.getHost().isBlank()) {
            ClientConfig cfg = new ClientConfig();
            String proxyUri = String.format("http://%s:%d", proxy.getHost(),
                    proxy.getPort() != null ? proxy.getPort() : 8080);
            cfg.property(ClientProperties.PROXY_URI, proxyUri);
            if (proxy.getUsername() != null && !proxy.getUsername().isBlank()) {
                cfg.property(ClientProperties.PROXY_USERNAME, proxy.getUsername());
                cfg.property(ClientProperties.PROXY_PASSWORD, proxy.getPassword());
            }
            Client client = ClientBuilder.newClient(cfg);
            apiClient.setHttpClient(client);
        }
    }
}
