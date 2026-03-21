package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.AccountsApi;
import com.unblu.webapi.jersey.v4.api.NamedAreasApi;
import com.unblu.webapi.jersey.v4.api.TeamsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.NamedAreaInfo;
import org.dbs.poc.unblu.domain.model.TeamInfo;
import org.dbs.poc.unblu.infrastructure.exception.UnbluApiException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles Unblu account, team and named area operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnbluService {

    private final ApiClient apiClient;

    private static final List<ExpandFields> NO_EXPAND = null;

    public Account getCurrentAccount() {
        try {
            AccountsApi accountsApi = new AccountsApi(apiClient);
            log.info("Fetching current account from Unblu...");
            Account result = accountsApi.accountsGetCurrentAccount(NO_EXPAND);
            log.info("Successfully fetched current account");
            return result;
        } catch (ApiException e) {
            log.error("Error fetching current account - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes pour accéder au compte actuel");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération du compte actuel : " + e.getMessage());
        }
    }

    public AccountResult listAccounts() {
        try {
            AccountsApi accountsApi = new AccountsApi(apiClient);
            AccountQuery query = new AccountQuery();
            log.info("Fetching accounts from Unblu...");
            AccountResult result = accountsApi.accountsSearch(query, NO_EXPAND);
            log.info("Found {} accounts", result.getItems().size());
            return result;
        } catch (ApiException e) {
            log.error("Error fetching accounts - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes pour rechercher les comptes");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche des comptes : " + e.getMessage());
        }
    }

    public List<TeamInfo> searchTeams() {
        try {
            TeamsApi teamsApi = new TeamsApi(apiClient);
            TeamQuery query = new TeamQuery();
            log.info("Récupération des équipes dans Unblu...");
            TeamResult result = teamsApi.teamsSearch(query, NO_EXPAND);
            log.info("Trouvé {} équipe(s)", result.getItems().size());
            return result.getItems().stream()
                    .map(team -> new TeamInfo(team.getId(), team.getName(), team.getDescription()))
                    .toList();
        } catch (ApiException e) {
            log.error("Erreur lors de la récupération des équipes - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération des équipes : " + e.getMessage());
        }
    }

    public List<NamedAreaInfo> searchNamedAreas() {
        try {
            NamedAreasApi namedAreasApi = new NamedAreasApi(apiClient);
            NamedAreaQuery query = new NamedAreaQuery();
            log.info("Récupération des zones nommées dans Unblu...");
            NamedAreaResult result = namedAreasApi.namedAreasSearch(query, NO_EXPAND);
            log.info("Trouvé {} zone(s) nommée(s)", result.getItems().size());
            return result.getItems().stream()
                    .map(namedArea -> new NamedAreaInfo(namedArea.getId(), namedArea.getName(), namedArea.getDescription()))
                    .toList();
        } catch (ApiException e) {
            log.error("Erreur lors de la récupération des zones nommées - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération des zones nommées : " + e.getMessage());
        }
    }
}
