package org.dbs.poc.unblu.integration.infrastructure.adapter.summary;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.integration.domain.port.out.ConversationSummaryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Adaptateur mock du port secondaire {@link ConversationSummaryPort}.
 * Génère des résumés de conversation aléatoires à partir de phrases prédéfinies.
 * N'utilise plus Camel pour la génération synchrone.
 */
@Slf4j
@Component
public class ConversationSummaryMockAdapter implements ConversationSummaryPort {


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

    /**
     * Génère un résumé aléatoire pour la conversation spécifiée en combinant deux phrases prédéfinies.
     *
     * @param conversationId l'identifiant de la conversation pour laquelle générer un résumé
     * @return le résumé généré (deux phrases séparées par un saut de ligne)
     */
    @Override
    @CircuitBreaker(name = "summary")
    public String generateSummary(String conversationId) {
        String summary = LINE1.get(random.nextInt(LINE1.size())) + "\n"
                + LINE2.get(random.nextInt(LINE2.size()));
        log.info("Résumé généré pour la conversation {}: {}", conversationId, summary);
        return summary;
    }

}
