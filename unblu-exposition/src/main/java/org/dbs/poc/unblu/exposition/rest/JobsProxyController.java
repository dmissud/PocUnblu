package org.dbs.poc.unblu.exposition.rest;

import org.dbs.poc.unblu.exposition.rest.config.ProxyHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Reverse proxy transparent vers JobsApplication (port 8085).
 * Nécessaire quand le frontend Angular est servi par UnbluApplication (port 8081) :
 * le navigateur cible /api/jobs/** sur 8081, ce contrôleur forwarde vers 8085.
 */
@RestController
@RequestMapping("/api/jobs")
public class JobsProxyController {

    private final RestTemplate restTemplate;
    private final String jobsBaseUrl;

    public JobsProxyController(RestTemplate restTemplate,
                               @Value("${jobs.base-url:http://localhost:8085}") String jobsBaseUrl) {
        this.restTemplate = restTemplate;
        this.jobsBaseUrl = jobsBaseUrl;
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String targetUrl = buildTargetUrl(request);

        HttpEntity<byte[]> entity = new HttpEntity<>(body, ProxyHeaders.extract(request));

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
        return jobsBaseUrl + path + (query != null ? "?" + query : "");
    }

}

// Made with Bob
