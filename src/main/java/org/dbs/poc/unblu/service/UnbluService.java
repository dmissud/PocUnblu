package org.dbs.poc.unblu.service;

import com.unblu.webapi.jersey.v3.ApiClient;
import com.unblu.webapi.jersey.v3.api.AccountsApi;
import com.unblu.webapi.model.v3.AccountQuery;
import com.unblu.webapi.model.v3.AccountResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnbluService {

    private final ApiClient apiClient;

    /**
     * Example method to list accounts from Unblu
     */
    public AccountResult listAccounts() {
        try {
            AccountsApi accountsApi = new AccountsApi(apiClient);
            AccountQuery query = new AccountQuery();
            
            log.info("Fetching accounts from Unblu...");
            AccountResult result = accountsApi.accountsSearch(query);
            log.info("Successfully fetched {} accounts", result.getItems().size());
            
            return result;
        } catch (Exception e) {
            log.error("Error fetching accounts from Unblu", e);
            throw new RuntimeException("Failed to fetch accounts", e);
        }
    }
}
