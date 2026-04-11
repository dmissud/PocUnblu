package org.dbs.poc.unblu.exposition.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.dbs.poc.unblu.exposition.rest.config.ProxyHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Reverse proxy transparent pour les callbacks webhook Unblu vers webhook-entrypoint (port 8083).
 *
 * <p>Unblu envoie POST /api/webhooks/unblu sur ce service (port 8081, exposé via ngrok).
 * Ce contrôleur forward l'appel vers le module dédié webhook-entrypoint.
 */
@RestController
public class WebhookEntrypointProxyController {

    private final RestTemplate restTemplate;
    private final String webhookEntrypointBaseUrl;

    public WebhookEntrypointProxyController(
            RestTemplate restTemplate,
            @Value("${webhook.entrypoint.base-url:http://localhost:8083}") String webhookEntrypointBaseUrl) {
        this.restTemplate = restTemplate;
        this.webhookEntrypointBaseUrl = webhookEntrypointBaseUrl;
    }

    @PostMapping("/api/webhooks/unblu")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String targetUrl = webhookEntrypointBaseUrl + request.getRequestURI();

        HttpEntity<byte[]> entity = new HttpEntity<>(body, ProxyHeaders.extract(request));

        try {
            return restTemplate.exchange(targetUrl, HttpMethod.POST, entity, byte[].class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());
        }
    }

}
