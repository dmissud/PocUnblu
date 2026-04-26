package org.dbs.poc.unblu.integration.domain.port.out;

import org.dbs.poc.unblu.integration.domain.model.ClientContext;

/**
 * Port secondaire vers le CRM/ERP.
 * Résout le contexte client à partir d'une conversation Unblu.
 */
public interface ClientContextPort {
    ClientContext resolveClientContext(String conversationId);
}
