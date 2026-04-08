package org.dbs.poc.unblu.domain.port.out;

/**
 * Port secondaire pour la gestion de l'enregistrement du bot PocBot dans la plateforme Unblu.
 */
public interface BotRegistrationPort {

    /**
     * Crée ou met à jour le bot PocBot avec l'URL ngrok publique fournie.
     * L'opération est idempotente : si le bot existe déjà, son endpoint est mis à jour.
     *
     * @param ngrokUrl URL publique ngrok (ex. https://xxxx.ngrok.io)
     * @return informations du bot enregistré
     */
    BotRegistration setupPocBot(String ngrokUrl);

    /**
     * Désactive le bot PocBot dans Unblu (webhook INACTIVE).
     * Utilisé lors du teardown pour éviter qu'Unblu appelle une URL ngrok périmée.
     */
    void deactivatePocBot();

    /**
     * Représentation d'un bot enregistré dans Unblu.
     *
     * @param id       identifiant unique du bot
     * @param name     nom du bot
     * @param endpoint URL de l'endpoint outbound requests
     */
    record BotRegistration(String id, String name, String endpoint) {}
}
