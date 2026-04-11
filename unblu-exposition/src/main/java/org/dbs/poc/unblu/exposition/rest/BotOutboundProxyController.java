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
 * Reverse proxy transparent pour les outbound requests bot Unblu vers livekit (port 8082).
 *
 * <p>Unblu envoie POST /api/bot/outbound sur ce service (port 8081, exposé via ngrok).
 * Ce contrôleur forward l'appel vers le module livekit qui gère la logique bot.
 */
@RestController
public class BotOutboundProxyController {

    private final RestTemplate restTemplate;
    private final String livekitBaseUrl;

    public BotOutboundProxyController(
            RestTemplate restTemplate,
            @Value("${livekit.base-url:http://localhost:8082}") String livekitBaseUrl) {
        this.restTemplate = restTemplate;
        this.livekitBaseUrl = livekitBaseUrl;
    }

    @PostMapping("/api/bot/outbound")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String targetUrl = livekitBaseUrl + request.getRequestURI();

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
