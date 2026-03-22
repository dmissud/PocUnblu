package org.dbs.poc.unblu.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriétés de configuration du client Unblu, lues depuis le préfixe {@code unblu.api}.
 * Contient les identifiants API, l'URL de base et les paramètres de proxy optionnels.
 */
@Data
@Component
@ConfigurationProperties(prefix = "unblu.api")
public class UnbluProperties {
    /**
     * URL de base de l'API Unblu (ex. {@code https://services8.unblu.com/app/rest/v4}).
     */
    private String baseUrl;
    /** Nom d'utilisateur pour l'authentification basique à l'API Unblu. */
    private String username;
    /** Mot de passe pour l'authentification basique à l'API Unblu. */
    private String password;
    /** Identifiant Unblu de la personne bot utilisée pour envoyer les résumés de conversation. */
    private String summaryBotPersonId;
    /** Nom du bot Unblu à créer ou utiliser (défaut : {@code Boby}). */
    private String botName = "Boby";

    /** Configuration du proxy HTTP, initialisée avec des valeurs vides par défaut. */
    private ProxyProperties proxy = new ProxyProperties();

    /**
     * Propriétés de configuration du proxy HTTP pour les appels à l'API Unblu.
     */
    @Data
    public static class ProxyProperties {
        /** Hôte du proxy HTTP. */
        private String host;
        /** Port du proxy HTTP. */
        private Integer port;
        /** Nom d'utilisateur pour l'authentification proxy (optionnel). */
        private String username;
        /** Mot de passe pour l'authentification proxy (optionnel). */
        private String password;
    }
}
