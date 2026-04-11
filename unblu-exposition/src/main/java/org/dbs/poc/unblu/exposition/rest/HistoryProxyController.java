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
 * Reverse proxy transparent vers le module unblu-event-processor (port 8084).
 * Nécessaire quand le frontend Angular est servi par UnbluApplication (port 8081) :
 * le navigateur cible /api/history/** sur 8081, ce contrôleur forwarde vers 8084.
 */
@RestController
@RequestMapping("/api/history")
public class HistoryProxyController {

    private final RestTemplate restTemplate;
    private final String engineBaseUrl;

    public HistoryProxyController(RestTemplate restTemplate,
                                  @Value("${integration.event-processor.base-url:http://localhost:8084}") String engineBaseUrl) {
        this.restTemplate = restTemplate;
        this.engineBaseUrl = engineBaseUrl;
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String targetUrl = buildTargetUrl(request);

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

    private String buildTargetUrl(HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        return engineBaseUrl + path + (query != null ? "?" + query : "");
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
