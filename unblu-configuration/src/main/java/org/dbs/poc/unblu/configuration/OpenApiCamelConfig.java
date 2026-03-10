package org.dbs.poc.unblu.configuration;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class OpenApiCamelConfig extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Setup REST DSL configuration avec OpenAPI
        restConfiguration()
            .component("servlet")
            .bindingMode(RestBindingMode.json)
            .dataFormatProperty("prettyPrint", "true")
            .apiContextPath("/openapi.json") // Point d'entrée pour la spec OpenAPI
            .apiProperty("api.title", "Unblu Camel API (Hexagonal Stricte)")
            .apiProperty("api.version", "1.0.0")
            .apiProperty("cors", "true");
    }
}
