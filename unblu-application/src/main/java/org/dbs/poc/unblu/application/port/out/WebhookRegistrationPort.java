package org.dbs.poc.unblu.application.port.out;

import java.util.List;

/**
 * Port for managing webhook registrations in the Unblu platform.
 */
public interface WebhookRegistrationPort {

    /**
     * Recherche un webhook par son nom.
     *
     * @param name nom du webhook
     * @return enregistrement du webhook trouvé
     * @throws org.dbs.poc.unblu.domain.exception.UnbluApiException avec code 404 si non trouvé
     */
    WebhookRegistration findByName(String name);

    /**
     * Crée un nouveau webhook dans Unblu.
     *
     * @param name     nom du webhook
     * @param endpoint URL publique du endpoint de réception
     * @param events   liste des types d'événements à écouter
     * @return enregistrement du webhook créé
     */
    WebhookRegistration create(String name, String endpoint, List<String> events);

    /**
     * Met à jour l'endpoint et les événements d'un webhook existant.
     *
     * @param id       identifiant du webhook à mettre à jour
     * @param endpoint nouvelle URL publique du endpoint
     * @param events   nouvelle liste des types d'événements
     * @return enregistrement du webhook mis à jour
     */
    WebhookRegistration update(String id, String endpoint, List<String> events);

    /**
     * Supprime un webhook de Unblu.
     *
     * @param id identifiant du webhook à supprimer
     */
    void delete(String id);

    /**
     * Représentation d'un webhook enregistré dans Unblu.
     *
     * @param id       identifiant unique du webhook
     * @param name     nom du webhook
     * @param endpoint URL de réception des événements
     */
    record WebhookRegistration(String id, String name, String endpoint) {}
}
