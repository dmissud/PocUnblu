package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.PersonInfo;

import java.util.List;

/**
 * Cas d'utilisation : recherche des agents dont la configuration de file inclut une zone nommée spécifique.
 */
public interface SearchAgentsByNamedAreaUseCase {

    /**
     * Retourne les agents qui ont la zone nommée donnée dans leur filtre de file d'attente Unblu.
     *
     * @param namedAreaId identifiant de la zone nommée
     * @return liste des agents correspondants
     */
    List<PersonInfo> searchAgentsByNamedArea(String namedAreaId);
}
