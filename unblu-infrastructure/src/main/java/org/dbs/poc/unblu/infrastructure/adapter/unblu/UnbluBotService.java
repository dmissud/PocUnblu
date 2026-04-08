package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.BotsApi;
import com.unblu.webapi.jersey.v4.api.PersonsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.infrastructure.config.UnbluProperties;
import org.dbs.poc.unblu.infrastructure.exception.UnbluApiException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles Unblu bot operations: create, idempotent create-or-get, setup PocBot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnbluBotService {

    private final ApiClient apiClient;
    private final UnbluProperties unbluProperties;

    private static final String POC_BOT_DESCRIPTION = "Bot d'onboarding PocUnblu";
    private static final String POC_BOT_ENDPOINT_PATH = "/api/bot/outbound";

    private static final List<ExpandFields> NO_EXPAND = null;

    /**
     * Crée ou met à jour le bot PocBot avec l'URL ngrok fournie.
     * Idempotent : si le bot existe, son endpoint et son statut sont mis à jour.
     *
     * @param ngrokUrl URL publique ngrok
     * @return les données du bot après création ou mise à jour
     */
    public CustomDialogBotData setupPocBot(String ngrokUrl) {
        String pocBotName = unbluProperties.getPocBotName();
        String botEndpoint = ngrokUrl + POC_BOT_ENDPOINT_PATH;
        log.info("Setting up bot '{}' with endpoint: {}", pocBotName, botEndpoint);

        BotsApi botsApi = new BotsApi(apiClient);

        try {
            DialogBotData existing = botsApi.botsGetByName(pocBotName);
            log.info("PocBot already exists (id={}), updating endpoint and activating...", existing.getId());
            CustomDialogBotData botData = (CustomDialogBotData) existing;
            botData.setWebhookEndpoint(botEndpoint);
            botData.setWebhookStatus(ERegistrationStatus.ACTIVE);
            botData.setOnboardingFilter(EBotDialogFilter.VISITORS);
            botData.setOnboardingOrder(unbluProperties.getPocBotOnboardingOrder());
            CustomDialogBotData updated = (CustomDialogBotData) botsApi.botsUpdate(botData);
            log.info("PocBot updated: id={}, endpoint={}", updated.getId(), updated.getWebhookEndpoint());
            return updated;
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                log.error("Unexpected error fetching PocBot - Status: {}", e.getCode(), e);
                throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération de PocBot : " + e.getMessage());
            }
        }

        log.info("Bot '{}' not found, creating bot person and bot...", pocBotName);
        return createPocBot(botsApi, pocBotName, botEndpoint);
    }

    /**
     * Désactive le bot PocBot dans Unblu (webhookStatus = INACTIVE).
     * Appelé lors du teardown pour éviter qu'Unblu appelle une URL ngrok périmée.
     */
    public void deactivatePocBot() {
        String pocBotName = unbluProperties.getPocBotName();
        log.info("Deactivating bot '{}'...", pocBotName);
        BotsApi botsApi = new BotsApi(apiClient);
        try {
            DialogBotData existing = botsApi.botsGetByName(pocBotName);
            CustomDialogBotData botData = (CustomDialogBotData) existing;
            botData.setWebhookStatus(ERegistrationStatus.INACTIVE);
            botsApi.botsUpdate(botData);
            log.info("PocBot deactivated (id={})", existing.getId());
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.info("PocBot not found in Unblu, nothing to deactivate");
            } else {
                log.error("Error deactivating PocBot - Status: {}", e.getCode(), e);
                throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la désactivation de PocBot : " + e.getMessage());
            }
        }
    }

    private CustomDialogBotData createPocBot(BotsApi botsApi, String pocBotName, String botEndpoint) {
        try {
            PersonsApi personsApi = new PersonsApi(apiClient);

            PersonData botPerson = new PersonData();
            botPerson.setDisplayName(pocBotName);
            botPerson.setSourceId(toSourceId(pocBotName));
            PersonData createdPerson = personsApi.personsCreateOrUpdateBot(botPerson, NO_EXPAND);
            log.info("Bot person '{}' created: id={}", pocBotName, createdPerson.getId());

            CustomDialogBotData botData = new CustomDialogBotData();
            botData.setName(pocBotName);
            botData.setDescription(POC_BOT_DESCRIPTION);
            botData.setType(EBotType.CUSTOM);
            botData.setBotPersonId(createdPerson.getId());
            botData.setOnboardingFilter(EBotDialogFilter.VISITORS);
            botData.setOffboardingFilter(EBotDialogFilter.NONE);
            botData.setOnboardingOrder(unbluProperties.getPocBotOnboardingOrder());
            botData.setWebhookStatus(ERegistrationStatus.ACTIVE);
            botData.setWebhookEndpoint(botEndpoint);
            botData.setWebhookApiVersion(EWebApiVersion.V4);
            botData.setOutboundTimeoutMillis(5000L);
            botData.setOnTimeoutBehavior(EBotDialogTimeoutBehavior.ABORT);
            botData.setRetryCount(3L);
            botData.setRetryDelay(1000L);

            CustomDialogBotData created = (CustomDialogBotData) botsApi.botsCreate(botData);
            log.info("PocBot created: id={}, endpoint={}", created.getId(), created.getWebhookEndpoint());
            return created;
        } catch (ApiException e) {
            log.error("Error creating PocBot - Status: {}", e.getCode(), e);
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création de PocBot : " + e.getMessage());
        }
    }

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
