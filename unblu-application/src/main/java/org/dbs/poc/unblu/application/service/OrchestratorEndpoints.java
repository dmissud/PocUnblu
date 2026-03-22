package org.dbs.poc.unblu.application.service;

/**
 * Constantes centralisant les URI des endpoints Camel {@code direct:} utilisés dans l'orchestration.
 * Référence unique pour éviter la duplication de chaînes entre les routes.
 */
public final class OrchestratorEndpoints {

    private OrchestratorEndpoints() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Endpoint de démarrage du workflow conversation avec équipe.
     */
    public static final String DIRECT_START_CONVERSATION = "direct:start-conversation";

    /** Endpoint de démarrage du workflow conversation directe. */
    public static final String DIRECT_START_DIRECT_CONVERSATION = "direct:start-direct-conversation";

    /** Endpoint de l'adaptateur ERP (profil client). */
    public static final String DIRECT_ERP_ADAPTER = "direct:erp-adapter";

    /** Endpoint du moteur de règles (décision de routage). */
    public static final String DIRECT_RULE_ENGINE_ADAPTER = "direct:rule-engine-adapter";

    /** Endpoint de l'adaptateur Unblu avec disjoncteur (circuit breaker). */
    public static final String DIRECT_UNBLU_ADAPTER_RESILIENT = "direct:unblu-adapter-resilient";

    /** Endpoint de recherche de personnes dans Unblu. */
    public static final String DIRECT_UNBLU_SEARCH_PERSONS = "direct:unblu-search-persons";

    /** Endpoint de recherche d'équipes dans Unblu. */
    public static final String DIRECT_UNBLU_SEARCH_TEAMS = "direct:unblu-search-teams";

    /** Endpoint du générateur de résumé de conversation. */
    public static final String DIRECT_CONVERSATION_SUMMARY_ADAPTER = "direct:conversation-summary-adapter";

    /** Endpoint d'ajout du résumé dans une conversation Unblu. */
    public static final String DIRECT_UNBLU_ADD_SUMMARY = "direct:unblu-add-summary";

    /** Endpoint interne d'ajout du résumé (avec disjoncteur). */
    public static final String DIRECT_UNBLU_ADD_SUMMARY_INTERNAL = "direct:unblu-add-summary-internal";

    /** Endpoint de création d'une conversation directe dans Unblu. */
    public static final String DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION = "direct:unblu-create-direct-conversation";

    /** Endpoint de synchronisation des conversations Unblu vers la base de données. */
    public static final String DIRECT_SYNC_CONVERSATIONS = "direct:sync-conversations";
}
