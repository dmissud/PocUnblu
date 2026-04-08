package org.dbs.poc.unblu.exposition.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;

/**
 * Reverse proxy transparent vers LiveKitApplication (port 8082) pour les outbound requests du bot PocBot.
 *
 * <p>Unblu appelle l'URL ngrok exposée sur le port 8081. Ce contrôleur forwarde la requête
 * vers le {@code BotOutboundController} du module livekit (port 8082) en préservant tous les
 * headers, notamment {@code x-unblu-service-name} utilisé pour le dispatch.
 */
@RestController
@RequestMapping("/api/bot/outbound")
public class BotOutboundProxyController {

    private final RestTemplate restTemplate;
    private final String livekitBaseUrl;

    public BotOutboundProxyController(RestTemplate restTemplate,
                                      @Value("${livekit.base-url:http://localhost:8082}") String livekitBaseUrl) {
        this.restTemplate = restTemplate;
        this.livekitBaseUrl = livekitBaseUrl;
    }

    @RequestMapping
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String targetUrl = livekitBaseUrl + "/api/bot/outbound";

        HttpHeaders headers = extractHeaders(request);
        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.exchange(targetUrl, HttpMethod.valueOf(request.getMethod()), entity, byte[].class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());
        }
    }

    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!"host".equalsIgnoreCase(name)) {
                headers.set(name, request.getHeader(name));
            }
        }
        return headers;
    }
}
