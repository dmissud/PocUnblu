package org.dbs.poc.unblu.config;

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
}
