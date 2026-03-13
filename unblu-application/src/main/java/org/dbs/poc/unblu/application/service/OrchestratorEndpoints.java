package org.dbs.poc.unblu.application.service;

public final class OrchestratorEndpoints {

    private OrchestratorEndpoints() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String DIRECT_START_CONVERSATION = "direct:start-conversation";
    public static final String DIRECT_START_DIRECT_CONVERSATION = "direct:start-direct-conversation";

    public static final String DIRECT_ERP_ADAPTER = "direct:erp-adapter";
    public static final String DIRECT_RULE_ENGINE_ADAPTER = "direct:rule-engine-adapter";
    public static final String DIRECT_UNBLU_ADAPTER_RESILIENT = "direct:unblu-adapter-resilient";
    public static final String DIRECT_UNBLU_SEARCH_PERSONS = "direct:unblu-search-persons";
    public static final String DIRECT_UNBLU_SEARCH_TEAMS = "direct:unblu-search-teams";
    public static final String DIRECT_CONVERSATION_SUMMARY_ADAPTER = "direct:conversation-summary-adapter";
    public static final String DIRECT_UNBLU_ADD_SUMMARY = "direct:unblu-add-summary";
    public static final String DIRECT_UNBLU_ADD_SUMMARY_INTERNAL = "direct:unblu-add-summary-internal";
    public static final String DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION = "direct:unblu-create-direct-conversation";
}
