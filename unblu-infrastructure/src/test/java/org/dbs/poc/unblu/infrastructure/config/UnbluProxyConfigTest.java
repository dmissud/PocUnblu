package org.dbs.poc.unblu.infrastructure.config;

import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import jakarta.ws.rs.client.Client;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "unblu.api.proxy.host=myproxy.com",
    "unblu.api.proxy.port=8888",
    "unblu.api.proxy.username=proxyuser",
    "unblu.api.proxy.password=proxypass",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false"
})
class UnbluProxyConfigTest {

    @Autowired
    private ApiClient apiClient;

    @Test
    void testProxyConfiguration() {
        assertNotNull(apiClient);
        Client httpClient = apiClient.getHttpClient();
        assertNotNull(httpClient);
        
        Object proxyUri = httpClient.getConfiguration().getProperty(ClientProperties.PROXY_URI);
        assertEquals("http://myproxy.com:8888", proxyUri);
        
        Object proxyUser = httpClient.getConfiguration().getProperty(ClientProperties.PROXY_USERNAME);
        assertEquals("proxyuser", proxyUser);
        
        Object proxyPass = httpClient.getConfiguration().getProperty(ClientProperties.PROXY_PASSWORD);
        assertEquals("proxypass", proxyPass);
    }
}
