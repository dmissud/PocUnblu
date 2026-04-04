package org.dbs.poc.unblu.domain.port.in;

import org.dbs.poc.unblu.domain.model.UnbluConversationSummary;
import org.dbs.poc.unblu.domain.port.in.query.SearchConversationsByStateQuery;

import java.util.List;

/**
 * Port d'entrée (use case) pour la recherche de conversations Unblu filtrées par état.
 * Retourne directement les données Unblu sans passer par la base locale.
 */
public interface SearchConversationsByStateUseCase {

    /**
     * Recherche les conversations correspondant à l'état demandé.
     *
     * @param query la query portant l'état souhaité
     * @return liste des résumés de conversations (jamais {@code null}, peut être vide)
     */
    List<UnbluConversationSummary> searchByState(SearchConversationsByStateQuery query);
}
