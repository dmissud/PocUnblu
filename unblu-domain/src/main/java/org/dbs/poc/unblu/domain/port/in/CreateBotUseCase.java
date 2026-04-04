package org.dbs.poc.unblu.domain.port.in;

/**
 * Cas d'utilisation : création d'un bot dans Unblu pour l'envoi automatique de résumés de conversation.
 */
public interface CreateBotUseCase {

    /**
     * Crée un bot de résumé dans Unblu.
     *
     * @param name        nom du bot
     * @param description description fonctionnelle du bot
     * @return identifiant de la personne bot créée ({@code botPersonId})
     */
    String createSummaryBot(String name, String description);
}
