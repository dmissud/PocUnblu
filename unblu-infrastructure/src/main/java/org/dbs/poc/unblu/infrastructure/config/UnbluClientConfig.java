package org.dbs.poc.unblu.infrastructure.config;

import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Spring du client Unblu SDK.
 * Crée et configure le bean {@link ApiClient} utilisé par tous les services Unblu.
 */
@Configuration
@RequiredArgsConstructor
public class UnbluClientConfig {

    private final UnbluProperties unbluProperties;

    /**
     * Crée le {@link ApiClient} Unblu configuré avec l'URL de base, les identifiants
     * et le proxy éventuel définis dans {@link UnbluProperties}.
     *
     * @return le client API Unblu prêt à l'emploi
     */
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

        configureProxy(apiClient);

        return apiClient;
    }

    /**
     * Configure un proxy HTTP sur le client Jersey si les propriétés {@code unblu.api.proxy.host}
     * et {@code unblu.api.proxy.port} sont renseignées.
     * Supporte également l'authentification proxy si {@code proxy.username} est défini.
     *
     * @param apiClient le client Unblu à configurer
     */
    private void configureProxy(ApiClient apiClient) {
        UnbluProperties.ProxyProperties proxyProps = unbluProperties.getProxy();
        if (proxyProps != null && proxyProps.getHost() != null && !proxyProps.getHost().isBlank()) {
            ClientConfig clientConfig = new ClientConfig();
            
            String proxyUri = String.format("http://%s:%d", proxyProps.getHost(), 
                proxyProps.getPort() != null ? proxyProps.getPort() : 8080);
            
            clientConfig.property(ClientProperties.PROXY_URI, proxyUri);
            
            if (proxyProps.getUsername() != null && !proxyProps.getUsername().isBlank()) {
                clientConfig.property(ClientProperties.PROXY_USERNAME, proxyProps.getUsername());
                clientConfig.property(ClientProperties.PROXY_PASSWORD, proxyProps.getPassword());
            }

            Client client = ClientBuilder.newClient(clientConfig);
            apiClient.setHttpClient(client);
        }
    }
}
