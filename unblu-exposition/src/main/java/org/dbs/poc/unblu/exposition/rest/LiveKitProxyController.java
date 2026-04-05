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
 * Reverse proxy transparent vers LiveKitApplication (port 8082).
 * Nécessaire quand le frontend Angular est servi par UnbluApplication (port 8081) :
 * le navigateur cible /api/v1/livekit/** sur 8081, ce contrôleur forwarde vers 8082.
 */
@RestController
@RequestMapping("/api/v1/livekit")
public class LiveKitProxyController {

    private final RestTemplate restTemplate;
    private final String livekitBaseUrl;

    public LiveKitProxyController(RestTemplate restTemplate,
                                  @Value("${livekit.base-url:http://localhost:8082}") String livekitBaseUrl) {
        this.restTemplate = restTemplate;
        this.livekitBaseUrl = livekitBaseUrl;
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
        return livekitBaseUrl + path + (query != null ? "?" + query : "");
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
