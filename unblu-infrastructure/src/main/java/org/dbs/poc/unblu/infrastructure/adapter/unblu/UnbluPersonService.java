package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.PersonsApi;
import com.unblu.webapi.jersey.v4.api.UsersApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.model.PersonInfo;
import org.dbs.poc.unblu.infrastructure.exception.UnbluApiException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service d'accès à l'API Unblu pour toutes les opérations liées aux personnes :
 * recherche par critères, récupération par source, agents par état et par zone nommée.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnbluPersonService {

    private final ApiClient apiClient;

    private static final List<ExpandFields> NO_EXPAND = null;

    /**
     * Recherche des personnes dans Unblu, avec filtres optionnels sur la source et le sourceId.
     *
     * @param sourceId     identifiant source de la personne (peut être {@code null})
     * @param personSource type de source ({@code VIRTUAL} ou {@code USER_DB}, peut être {@code null})
     * @return liste des personnes correspondantes
     */
    public List<PersonInfo> searchPersons(String sourceId, org.dbs.poc.unblu.domain.model.PersonSource personSource) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            PersonQuery query = new PersonQuery();

            if (personSource != null) {
                EPersonSource ePersonSource = EPersonSource.valueOf(personSource.name());
                EqualsPersonSourceOperator sourceOperator = new EqualsPersonSourceOperator();
                sourceOperator.setValue(ePersonSource);
                PersonSourcePersonSearchFilter sourceFilter = new PersonSourcePersonSearchFilter();
                sourceFilter.setField(EPersonSearchFilterField.PERSON_SOURCE);
                sourceFilter.setOperator(sourceOperator);
                query.addSearchFiltersItem(sourceFilter);
            }

            if (sourceId != null && !sourceId.isBlank()) {
                EqualsIdOperator sourceIdOperator = new EqualsIdOperator();
                sourceIdOperator.setValue(sourceId);
                SourceIdPersonSearchFilter sourceIdFilter = new SourceIdPersonSearchFilter();
                sourceIdFilter.setField(EPersonSearchFilterField.SOURCE_ID);
                sourceIdFilter.setOperator(sourceIdOperator);
                query.addSearchFiltersItem(sourceIdFilter);
            }

            log.info("Recherche de personnes dans Unblu, sourceId: {}", sourceId);
            PersonResult result = personsApi.personsSearch(query, NO_EXPAND);
            log.info("Trouvé {} personne(s)", result.getItems().size());

            return result.getItems().stream().map(this::toPersonInfo).toList();
        } catch (ApiException e) {
            log.error("Erreur lors de la recherche de personnes dans Unblu - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche de personnes : " + e.getMessage());
        }
    }

    /**
     * Récupère les données d'une personne Unblu par son type de source et son identifiant source.
     *
     * @param personSource le type de source Unblu ({@link EPersonSource})
     * @param sourceId     l'identifiant source de la personne
     * @return les données de la personne trouvée
     * @throws org.dbs.poc.unblu.infrastructure.exception.UnbluApiException si la personne n'existe pas (404) ou accès refusé (403)
     */
    public PersonData getPersonBySource(EPersonSource personSource, String sourceId) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            log.info("Getting person by source - Source: {}, SourceId: {}", personSource, sourceId);
            PersonData result = personsApi.personsGetBySource(personSource, sourceId, NO_EXPAND);
            log.info("Found person with ID: {}", result.getId());
            return result;
        } catch (ApiException e) {
            log.error("Error getting person by source - Status: {}", e.getCode(), e);
            if (e.getCode() == 404) {
                throw new UnbluApiException(404, "Not Found", "Aucune personne trouvée avec cette source");
            }
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes pour rechercher une personne");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche de la personne : " + e.getMessage());
        }
    }

    /**
     * Recherche des agents Unblu selon une requête typée.
     *
     * @param query la requête de recherche d'agents
     * @return le résultat paginé des agents
     * @throws org.dbs.poc.unblu.infrastructure.exception.UnbluApiException en cas d'erreur API
     */
    public PersonResult searchAgents(PersonTypedQuery query) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            log.info("Searching for agents in Unblu...");
            PersonResult result = personsApi.personsSearchAgents(query, NO_EXPAND);
            log.info("Found {} agents", result.getItems().size());
            return result;
        } catch (ApiException e) {
            log.error("Error searching agents - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes pour rechercher les agents");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche des agents : " + e.getMessage());
        }
    }

    /**
     * Recherche des agents Unblu filtrés par état de disponibilité.
     *
     * @param query la requête incluant les critères d'état
     * @return le résultat paginé des agents par état
     * @throws org.dbs.poc.unblu.infrastructure.exception.UnbluApiException en cas d'erreur API
     */
    public AgentPersonStateResult searchAgentsByState(AgentStateQuery query) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            log.info("Searching for agents by state...");
            AgentPersonStateResult result = personsApi.personsSearchAgentsByState(query);
            log.info("Found {} agents by state", result.getItems().size());
            return result;
        } catch (ApiException e) {
            log.error("Error searching agents by state - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) {
                throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes pour rechercher les agents par état");
            }
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche des agents par état : " + e.getMessage());
        }
    }

    /**
     * Recherche les agents dont la configuration de file contient la zone nommée spécifiée.
     * Utilise le filtre {@code com.unblu.queue.ui.defaultFilterNamedAreas} de la configuration utilisateur.
     *
     * @param namedAreaId l'identifiant de la zone nommée à filtrer
     * @return liste des agents configurés sur cette zone nommée
     * @throws org.dbs.poc.unblu.infrastructure.exception.UnbluApiException en cas d'erreur API
     */
    public List<PersonInfo> searchAgentsByNamedArea(String namedAreaId) {
        try {
            UsersApi usersApi = new UsersApi(apiClient);
            UserQuery query = new UserQuery();

            log.info("Recherche des agents ayant la named area {} dans leur configuration de queue...", namedAreaId);

            List<ExpandFields> expand = List.of(ExpandFields.CONFIGURATION);
            UserResult result = usersApi.usersSearch(query, expand);

            log.info("Trouvé {} utilisateur(s) au total", result.getItems().size());

            List<PersonInfo> agents = result.getItems().stream()
                    .filter(user -> {
                        Map<String, String> config = user.getConfiguration();
                        if (config == null) return false;
                        String namedAreasFilter = config.get("com.unblu.queue.ui.defaultFilterNamedAreas");
                        return namedAreasFilter != null && namedAreasFilter.contains(namedAreaId);
                    })
                    .map(user -> new PersonInfo(
                            user.getId(),
                            user.getUsername(),
                            user.getDisplayName(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            "AGENT"))
                    .toList();

            log.info("Trouvé {} agent(s) avec la named area {}", agents.size(), namedAreaId);
            return agents;
        } catch (ApiException e) {
            log.error("Erreur lors de la recherche des agents par named area - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche des agents par named area : " + e.getMessage());
        }
    }

    /**
     * Convertit un {@link PersonData} du SDK Unblu en {@link PersonInfo} du domaine.
     *
     * @param personData les données de la personne retournées par l'API Unblu
     * @return le modèle de domaine correspondant
     */
    PersonInfo toPersonInfo(PersonData personData) {
        return new PersonInfo(
                personData.getId(),
                personData.getSourceId(),
                personData.getDisplayName(),
                personData.getEmail(),
                personData.getFirstName(),
                personData.getLastName(),
                personData.getPersonType() != null ? personData.getPersonType().name() : null);
    }
}
