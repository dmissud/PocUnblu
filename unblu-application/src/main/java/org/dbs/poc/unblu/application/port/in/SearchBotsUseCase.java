package org.dbs.poc.unblu.application.port.in;

import org.dbs.poc.unblu.domain.model.BotInfo;

import java.util.List;

/**
 * Cas d'utilisation : liste et supervision des bots Unblu.
 */
public interface SearchBotsUseCase {

    /**
     * Retourne la liste de tous les bots configurés dans Unblu.
     *
     * @return liste des informations de supervision de bots
     */
    List<BotInfo> listBots();
}
