package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.PersonsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.infrastructure.exception.UnbluApiException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles Unblu bot operations: create, idempotent create-or-get.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnbluBotService {

    private final ApiClient apiClient;

    private static final List<ExpandFields> NO_EXPAND = null;

    /**
     * Creates a bot in Unblu. Returns the botPersonId.
     */
    public String createBot(String name, String description) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            BotsApi botsApi = new BotsApi(apiClient);

            PersonData botPerson = new PersonData();
            botPerson.setDisplayName(name);
            botPerson.setSourceId(toSourceId(name));
            PersonData createdPerson = personsApi.personsCreateOrUpdateBot(botPerson, NO_EXPAND);
            String botPersonId = createdPerson.getId();
            log.info("Personne bot créée: id={}", botPersonId);

            CustomDialogBotData botData = new CustomDialogBotData();
            botData.setName(name);
            botData.setDescription(description);
            botData.setType(EBotType.CUSTOM);
            botData.setBotPersonId(botPersonId);
            botData.setOnboardingFilter(EBotDialogFilter.NONE);
            botData.setOffboardingFilter(EBotDialogFilter.NONE);
            botData.setWebhookStatus(ERegistrationStatus.INACTIVE);
            botData.setWebhookEndpoint("http://localhost/unused");
            botData.setWebhookApiVersion(EWebApiVersion.V4);
            botData.setOutboundTimeoutMillis(5000L);
            botsApi.botsCreate(botData);
            log.info("Bot créé: name={}, botPersonId={}", name, botPersonId);

            return botPersonId;
        } catch (ApiException e) {
            log.error("Erreur lors de la création du bot - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création du bot : " + e.getMessage());
        }
    }

    /**
     * Creates a bot or returns the existing one if already present (idempotent).
     */
    public String createOrGetBot(String name, String description) {
        String sourceId = toSourceId(name);
        String existingBotId = findBotPersonIdBySourceId(sourceId);
        if (existingBotId != null) {
            log.info("Bot déjà existant: name={}, sourceId={}, botPersonId={}", name, sourceId, existingBotId);
            return existingBotId;
        }
        return createBot(name, description);
    }

    private String findBotPersonIdBySourceId(String sourceId) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);
            PersonData person = personsApi.personsGetBySource(EPersonSource.VIRTUAL, sourceId, NO_EXPAND);
            log.info("Bot trouvé avec sourceId={}, personId={}", sourceId, person.getId());
            return person.getId();
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.info("Aucun bot trouvé avec sourceId={}", sourceId);
                return null;
            }
            log.error("Erreur lors de la recherche du bot - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche du bot : " + e.getMessage());
        }
    }

    private static String toSourceId(String name) {
        return "bot-" + name.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }
}
