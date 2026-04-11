package org.dbs.poc.unblu.exposition.rest.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Enumeration;
import java.util.Set;

/**
 * Utilitaire de filtrage des headers HTTP pour les reverse proxies.
 *
 * <p>Les headers suivants sont exclus de la retransmission :
 * <ul>
 *   <li><b>host</b> — doit correspondre au backend cible, pas au client appelant</li>
 *   <li><b>content-length</b> — recalculé par RestTemplate à partir du body effectif ;
 *       la valeur d'origine peut diverger si ngrok a modifié l'encodage en transit</li>
 *   <li><b>transfer-encoding</b> — idem, géré par le transport HTTP sous-jacent</li>
 *   <li><b>connection</b> — directive point-à-point, ne doit pas être propagée</li>
 *   <li><b>accept-encoding</b> — Unblu envoie {@code Accept-Encoding: gzip} sur ses
 *       outbound requests bot. Si ce header est transmis au backend (livekit), Tomcat
 *       répond en gzip. Unblu interprète alors la réponse comme du JSON plain et échoue
 *       à la parser → pas de réponse exploitable. En supprimant ce header, le backend
 *       répond toujours en JSON non compressé. Note : livekit désactive aussi la
 *       compression côté serveur ({@code server.compression.enabled=false}) pour couvrir
 *       le cas d'un appel direct sans proxy.</li>
 * </ul>
 */
public final class ProxyHeaders {

    private static final Set<String> EXCLUDED = Set.of(
            "host",
            "content-length",
            "transfer-encoding",
            "connection",
            "accept-encoding"
    );

    private ProxyHeaders() {}

    public static HttpHeaders extract(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!EXCLUDED.contains(name.toLowerCase())) {
                headers.set(name, request.getHeader(name));
            }
        }
        return headers;
    }
}
