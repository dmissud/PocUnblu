package org.dbs.poc.unblu.integration.domain.port.out;

import org.dbs.poc.unblu.integration.domain.model.ClientContext;

/**
 * Port secondaire vers le système de génération d'URLs contextuelles.
 * Calcule l'URL d'une ressource (document, espace client, co-browsing) à positionner dans Unblu.
 */
public interface ResourceUrlPort {
    String computeResourceUrl(ClientContext clientContext);
}
