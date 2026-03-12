package org.dbs.poc.unblu.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "unblu.api")
public class UnbluProperties {
    private String baseUrl;
    private String username;
    private String password;
    private String summaryBotPersonId;

    private ProxyProperties proxy = new ProxyProperties();

    @Data
    public static class ProxyProperties {
        private String host;
        private Integer port;
        private String username;
        private String password;
    }
}
