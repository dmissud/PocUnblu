package org.dbs.poc.unblu.exposition.rest;

import jakarta.servlet.http.HttpServletRequest;
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

import java.util.Enumeration;

/**
 * Transparent reverse proxy for Unblu webhook callbacks towards WebhookEntrypointApplication (port 8083).
 *
 * <p>Unblu sends POST /api/webhooks/unblu to this application (port 8081, exposed via ngrok).
 * This controller forwards the call to the dedicated webhook-entrypoint module.
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

        HttpHeaders headers = extractHeaders(request);
        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.exchange(targetUrl, HttpMethod.POST, entity, byte[].class);
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
