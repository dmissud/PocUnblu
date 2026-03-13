package org.dbs.poc.unblu.infrastructure.adapter.summary;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.domain.port.secondary.ConversationSummaryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class ConversationSummaryMockAdapter extends RouteBuilder implements ConversationSummaryPort {

    private static final List<String> LINE1 = List.of(
            "Le client a contacté le service pour une demande d'information sur ses produits.",
            "Le client souhaite obtenir un accompagnement personnalisé sur ses placements financiers.",
            "Le client a signalé un problème technique lié à l'accès à son espace personnel.",
            "Le client demande une révision de son contrat en cours.",
            "Le client cherche des conseils pour optimiser sa situation patrimoniale."
    );

    private static final List<String> LINE2 = List.of(
            "Un conseiller spécialisé a été assigné pour traiter la demande en priorité.",
            "La demande nécessite une analyse approfondie avant toute réponse définitive.",
            "Le dossier a été transmis à l'équipe compétente pour prise en charge rapide.",
            "Une réponse personnalisée sera fournie dans les meilleurs délais.",
            "Le conseiller prendra contact avec le client sous 24 heures pour un suivi."
    );

    private final Random random = new Random();

    @Override
    public String generateSummary(String conversationId) {
        String summary = LINE1.get(random.nextInt(LINE1.size())) + "\n"
                + LINE2.get(random.nextInt(LINE2.size()));
        log.info("Résumé généré pour la conversation {}: {}", conversationId, summary);
        return summary;
    }

    @Override
    public void configure() throws Exception {
        from("direct:conversation-summary-adapter")
            .routeId("mock-conversation-summary-adapter")
            .log("Génération du résumé pour la conversation: ${header.CamelBeanMethodArgs[0]}")
            .bean(this, "generateSummary");
    }
}
