package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.TeamInfo;

import java.util.List;

/**
 * Cas d'utilisation : recherche de toutes les équipes disponibles dans Unblu.
 */
public interface SearchTeamsUseCase {

    /**
     * Retourne la liste de toutes les équipes Unblu.
     *
     * @return liste des équipes
     */
    List<TeamInfo> searchTeams();
}
