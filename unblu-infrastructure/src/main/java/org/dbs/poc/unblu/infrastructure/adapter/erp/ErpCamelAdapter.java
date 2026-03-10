package org.dbs.poc.unblu.infrastructure.adapter.erp;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.port.secondary.ErpPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ErpCamelAdapter implements ErpPort {

    private final ProducerTemplate producerTemplate;

    @Override
    public CustomerProfile getCustomerProfile(String clientId) {
        // We create a temporary context just for the Camel route to extract the ID
        ConversationContext requestContext = ConversationContext.builder()
                .initialClientId(clientId)
                .build();
                
        // Call the resilient Camel route for ERP
        return producerTemplate.requestBody("direct:erp-adapter", requestContext, CustomerProfile.class);
    }
}
