package org.dbs.poc.unblu.infrastructure.adapter.erp;

import lombok.RequiredArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.port.out.ErpPort;
import org.springframework.stereotype.Component;

/**
 * Adaptateur secondaire implémentant {@link ErpPort} en déléguant l'appel ERP
 * à la route Camel {@code direct:erp-adapter} via un {@link ProducerTemplate}.
 */
@Component
@RequiredArgsConstructor
public class ErpCamelAdapter implements ErpPort {

    private final ProducerTemplate producerTemplate;

    /**
     * Récupère le profil client depuis l'ERP via la route Camel correspondante.
     *
     * @param clientId l'identifiant du client dont on souhaite récupérer le profil
     * @return le profil client retourné par l'ERP
     */
    @Override
    public CustomerProfile getCustomerProfile(String clientId) {
        // We create a temporary context just for the Camel route to extract the ID
        ConversationContext requestContext = new ConversationContext(clientId, "ERP_ADAPTER_REPLY");
                
        // Call the resilient Camel route for ERP
        return producerTemplate.requestBody("direct:erp-adapter", requestContext, CustomerProfile.class);
    }
}
