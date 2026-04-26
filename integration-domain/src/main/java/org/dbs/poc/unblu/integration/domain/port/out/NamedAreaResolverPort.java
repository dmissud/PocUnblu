package org.dbs.poc.unblu.integration.domain.port.out;

import org.dbs.poc.unblu.integration.domain.model.ClientContext;

/**
 * Port secondaire vers le moteur de règles de routage.
 * Détermine le named area Unblu cible en fonction du contexte client.
 */
public interface NamedAreaResolverPort {
    String resolveNamedAreaId(ClientContext clientContext);
}
