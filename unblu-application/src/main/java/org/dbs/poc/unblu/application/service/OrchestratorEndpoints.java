package org.dbs.poc.unblu.application.service;

public interface OrchestratorEndpoints {
    String DIRECT_START_CONVERSATION = "direct:start-conversation";
    String DIRECT_START_DIRECT_CONVERSATION = "direct:start-direct-conversation";
    
    String DIRECT_ERP_ADAPTER = "direct:erp-adapter";
    String DIRECT_RULE_ENGINE_ADAPTER = "direct:rule-engine-adapter";
    String DIRECT_UNBLU_ADAPTER_RESILIENT = "direct:unblu-adapter-resilient";
    String DIRECT_UNBLU_SEARCH_PERSONS = "direct:unblu-search-persons";
    String DIRECT_CONVERSATION_SUMMARY_ADAPTER = "direct:conversation-summary-adapter";
    String DIRECT_UNBLU_ADD_SUMMARY = "direct:unblu-add-summary";
    String DIRECT_UNBLU_ADD_SUMMARY_INTERNAL = "direct:unblu-add-summary-internal";
    String DIRECT_UNBLU_CREATE_DIRECT_CONVERSATION = "direct:unblu-create-direct-conversation";
}
