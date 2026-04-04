package org.dbs.poc.unblu.domain.port.in;

import org.dbs.poc.unblu.domain.model.NamedAreaInfo;

import java.util.List;

/**
 * Cas d'utilisation : recherche de toutes les zones nommées (named areas) disponibles dans Unblu.
 */
public interface SearchNamedAreasUseCase {

    /**
     * Retourne la liste de toutes les zones nommées Unblu.
     *
     * @return liste des zones nommées
     */
    List<NamedAreaInfo> searchNamedAreas();
}
