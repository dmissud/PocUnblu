package org.dbs.poc.unblu.infrastructure.adapter.unblu;

import com.unblu.webapi.jersey.v4.api.WebhookRegistrationsApi;
import com.unblu.webapi.jersey.v4.invoker.ApiClient;
import com.unblu.webapi.jersey.v4.invoker.ApiException;
import com.unblu.webapi.model.v4.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.infrastructure.exception.UnbluApiException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles Unblu webhook registration operations: CRUD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnbluWebhookService {

    private final ApiClient apiClient;

    public WebhookRegistrationResult searchWebhooks(WebhookRegistrationQuery query) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);
            log.info("Searching for webhooks in Unblu...");
            WebhookRegistrationResult result = webhookApi.webhookRegistrationsSearch(query);
            log.info("Found {} webhooks", result.getItems().size());
            return result;
        } catch (ApiException e) {
            log.error("Error searching webhooks - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes pour rechercher les webhooks");
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la recherche des webhooks : " + e.getMessage());
        }
    }

    public WebhookRegistration getWebhookById(String registrationId) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);
            log.info("Getting webhook by ID: {}", registrationId);
            WebhookRegistration result = webhookApi.webhookRegistrationsRead(registrationId);
            log.info("Found webhook: {}", result.getName());
            return result;
        } catch (ApiException e) {
            log.error("Error getting webhook by ID - Status: {}", e.getCode(), e);
            if (e.getCode() == 404) throw new UnbluApiException(404, "Not Found", "Webhook non trouvé");
            if (e.getCode() == 403) throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes");
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération du webhook : " + e.getMessage());
        }
    }

    public WebhookRegistration getWebhookByName(String name) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);
            log.info("Getting webhook by name: {}", name);
            WebhookRegistration result = webhookApi.webhookRegistrationsGetByName(name);
            log.info("Found webhook: {}", result.getName());
            return result;
        } catch (ApiException e) {
            log.error("Error getting webhook by name - Status: {}", e.getCode(), e);
            if (e.getCode() == 404) throw new UnbluApiException(404, "Not Found", "Webhook non trouvé");
            if (e.getCode() == 403) throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes");
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la récupération du webhook : " + e.getMessage());
        }
    }

    public WebhookRegistration createWebhook(String name, String endpoint, List<String> eventTypes) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);
            log.info("Creating webhook - Name: {}, Endpoint: {}, Events: {}", name, endpoint, eventTypes);

            WebhookRegistration webhookData = new WebhookRegistration();
            webhookData.setName(name);
            webhookData.setDescription("Webhook auto-configuré pour le PoC Unblu");
            webhookData.setEndpoint(endpoint);
            webhookData.setApiVersion(EWebApiVersion.V4);
            webhookData.setStatus(ERegistrationStatus.ACTIVE);
            webhookData.setEvents(eventTypes);

            WebhookRegistration result = webhookApi.webhookRegistrationsCreate(webhookData);
            log.info("Webhook créé: {} (ID: {})", result.getName(), result.getId());
            return result;
        } catch (ApiException e) {
            log.error("Error creating webhook - Status: {}", e.getCode(), e);
            if (e.getCode() == 403) throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes pour créer un webhook");
            if (e.getCode() == 409) throw new UnbluApiException(409, "Conflict", "Un webhook avec ce nom existe déjà");
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la création du webhook : " + e.getMessage());
        }
    }

    public WebhookRegistration updateWebhook(String webhookId, String endpoint, List<String> eventTypes) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);
            log.info("Updating webhook ID: {}, New Endpoint: {}", webhookId, endpoint);

            WebhookRegistration existing = webhookApi.webhookRegistrationsRead(webhookId);
            existing.setEndpoint(endpoint);
            existing.setStatus(ERegistrationStatus.ACTIVE);
            if (eventTypes != null) {
                existing.setEvents(eventTypes);
            }

            WebhookRegistration result = webhookApi.webhookRegistrationsUpdate(existing);
            log.info("Webhook mis à jour: {}", result.getId());
            return result;
        } catch (ApiException e) {
            log.error("Error updating webhook - Status: {}", e.getCode(), e);
            if (e.getCode() == 404) throw new UnbluApiException(404, "Not Found", "Webhook non trouvé");
            if (e.getCode() == 403) throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes");
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la mise à jour du webhook : " + e.getMessage());
        }
    }

    public void deleteWebhook(String webhookId) {
        try {
            WebhookRegistrationsApi webhookApi = new WebhookRegistrationsApi(apiClient);
            log.info("Deleting webhook ID: {}", webhookId);
            webhookApi.webhookRegistrationsDelete(webhookId);
            log.info("Webhook supprimé: {}", webhookId);
        } catch (ApiException e) {
            log.error("Error deleting webhook - Status: {}", e.getCode(), e);
            if (e.getCode() == 404) throw new UnbluApiException(404, "Not Found", "Webhook non trouvé");
            if (e.getCode() == 403) throw new UnbluApiException(403, "Forbidden", "Permissions insuffisantes");
            throw new UnbluApiException(e.getCode(), "Error", "Erreur lors de la suppression du webhook : " + e.getMessage());
        }
    }
}
