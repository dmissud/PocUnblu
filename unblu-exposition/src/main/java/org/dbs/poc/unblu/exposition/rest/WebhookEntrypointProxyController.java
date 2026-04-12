package org.dbs.poc.unblu.exposition.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.exposition.rest.config.ProxyHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
@Slf4j
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

        log.debug("Webhook proxy → {}", targetUrl);
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, byte[].class);
            log.debug("Webhook proxy ← {} ({} bytes)", response.getStatusCode(),
                    response.getBody() != null ? response.getBody().length : 0);
            HttpHeaders responseHeaders = new HttpHeaders();
            if (response.getHeaders().getContentType() != null) {
                responseHeaders.setContentType(response.getHeaders().getContentType());
            }
            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("Webhook proxy ← HTTP error {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            log.error("Webhook proxy — erreur inattendue vers {}: {}", targetUrl, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(("Proxy error: " + e.getMessage()).getBytes());
        }
    }

}
