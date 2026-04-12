package org.dbs.poc.unblu.infrastructure.adapter.ngrok;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.port.out.TunnelPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Adapter implementing {@link TunnelPort} using ngrok as the underlying tunnel provider.
 *
 * <p>Active uniquement avec le profil Spring {@code ngrok}.
 *
 * <p>Démarre un seul tunnel ngrok sur le port d'unblu-configuration (8081).
 * Les deux routes entrantes sont routées par les proxy controllers déjà en place :
 * <ul>
 *   <li>{@code POST /api/webhooks/unblu} → WebhookEntrypointProxyController → 8083</li>
 *   <li>{@code POST /api/bot/outbound}   → BotOutboundProxyController → 8082</li>
 * </ul>
 *
 * <p>Requires an ngrok authtoken configured ({@code ngrok config add-authtoken <token>}).
 */
@Slf4j
@Service
@Profile("ngrok")
public class NgrokManager implements TunnelPort {

    @Value("${unblu.webhook.local-port:8081}")
    private int localPort;

    private Process ngrokProcess;
    private String publicUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // TunnelPort implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean start() {
        return startNgrok();
    }

    @Override
    public void stop() {
        stopNgrok();
    }

    @Override
    public String getPublicUrl() {
        if (publicUrl != null) {
            return publicUrl;
        }
        fetchPublicUrl();
        return publicUrl;
    }

    @Override
    public String getBotPublicUrl() {
        return getPublicUrl();
    }

    @Override
    public TunnelStatus getStatus() {
        boolean running = isNgrokRunning();
        if (running) {
            fetchPublicUrl();
        }
        return new TunnelStatus(running, publicUrl, publicUrl);
    }

    // -------------------------------------------------------------------------
    // Internal lifecycle
    // -------------------------------------------------------------------------

    public boolean startNgrok() {
        if (isNgrokRunning()) {
            log.info("Ngrok is already running");
            fetchPublicUrl();
            return true;
        }

        if (!isNgrokInstalled()) {
            log.error("Ngrok is not installed. Please install ngrok and configure an authtoken.");
            return false;
        }

        try {
            Path configFile = writeNgrokConfig();
            log.info("Starting ngrok tunnel (unblu-configuration:{})...", localPort);

            // Pass both the default config (authtoken) and our tunnel config
            String defaultConfig = resolveDefaultNgrokConfig();
            ProcessBuilder pb = defaultConfig != null
                    ? new ProcessBuilder("ngrok", "start", "--all",
                    "--config", defaultConfig,
                    "--config", configFile.toString())
                    : new ProcessBuilder("ngrok", "start", "--all",
                    "--config", configFile.toString());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            ngrokProcess = pb.start();

            // Poll until tunnel is ready (up to 15 seconds)
            for (int i = 0; i < 15; i++) {
                Thread.sleep(1000);
                if (!ngrokProcess.isAlive()) {
                    log.error("Ngrok process exited unexpectedly (exit code: {})", ngrokProcess.exitValue());
                    return false;
                }
                fetchPublicUrl();
                if (publicUrl != null) {
                    log.info("Ngrok tunnel ready — {}", publicUrl);
                    return true;
                }
                log.debug("Waiting for ngrok tunnel... ({}/15)", i + 1);
            }

            log.error("Ngrok started but tunnel not ready after 15 seconds");
            stopNgrok();
            return false;

        } catch (Exception e) {
            log.error("Error starting ngrok", e);
            stopNgrok();
            return false;
        }
    }

    public void stopNgrok() {
        if (ngrokProcess != null && ngrokProcess.isAlive()) {
            log.info("Stopping ngrok tunnels...");
            ngrokProcess.destroy();
            try {
                ngrokProcess.waitFor(5, TimeUnit.SECONDS);
                if (ngrokProcess.isAlive()) {
                    log.warn("Ngrok didn't stop gracefully, forcing kill...");
                    ngrokProcess.destroyForcibly();
                }
                log.info("Ngrok tunnels stopped");
            } catch (InterruptedException e) {
                log.error("Error waiting for ngrok to stop", e);
                Thread.currentThread().interrupt();
            }
        }
        ngrokProcess = null;
        publicUrl = null;
    }

    public boolean isNgrokRunning() {
        if (ngrokProcess != null && ngrokProcess.isAlive()) {
            return true;
        }
        return fetchPublicUrlsFromApi() != null;
    }

    // -------------------------------------------------------------------------
    // Ngrok config file generation
    // -------------------------------------------------------------------------

    /**
     * Writes a temporary ngrok config file with a single tunnel on unblu-configuration (8081).
     * Webhook and bot routes are proxied internally by the exposition layer.
     */
    private Path writeNgrokConfig() throws IOException {
        String config = String.format("""
                version: "3"
                tunnels:
                  main:
                    addr: %d
                    proto: http
                """, localPort);

        Path configFile = Files.createTempFile("ngrok-poc-", ".yml");
        Files.writeString(configFile, config);
        configFile.toFile().deleteOnExit();
        log.debug("Ngrok config written to: {}", configFile);
        return configFile;
    }

    // -------------------------------------------------------------------------
    // URL discovery via ngrok local API
    // -------------------------------------------------------------------------

    /**
     * Fetches the tunnel URL from the ngrok local API and updates the cached field.
     */
    private void fetchPublicUrl() {
        JsonNode tunnels = fetchPublicUrlsFromApi();
        if (tunnels == null) return;

        for (JsonNode tunnel : tunnels) {
            String url = tunnel.path("public_url").asText("");
            if (url.startsWith("https://")) {
                publicUrl = url;
                log.debug("Ngrok tunnel URL: {}", publicUrl);
                return;
            }
        }
    }

    private JsonNode fetchPublicUrlsFromApi() {
        try {
            URL url = new URL("http://localhost:4040/api/tunnels");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) sb.append(line);
                    JsonNode root = objectMapper.readTree(sb.toString());
                    JsonNode tunnels = root.get("tunnels");
                    if (tunnels != null && tunnels.isArray() && !tunnels.isEmpty()) {
                        return tunnels;
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Could not reach ngrok API (ngrok may not be running)", e);
        }
        return null;
    }

    /**
     * Resolves the path to the default ngrok config file (contains the authtoken).
     * Returns null if not found.
     */
    private String resolveDefaultNgrokConfig() {
        String[] candidates = {
                System.getProperty("user.home") + "/.config/ngrok/ngrok.yml",
                System.getProperty("user.home") + "/Library/Application Support/ngrok/ngrok.yml",
                System.getProperty("user.home") + "/.ngrok2/ngrok.yml"
        };
        for (String path : candidates) {
            if (new java.io.File(path).exists()) {
                log.debug("Found default ngrok config: {}", path);
                return path;
            }
        }
        log.warn("Default ngrok config not found, authtoken may be missing");
        return null;
    }

    public boolean isNgrokInstalled() {
        try {
            Process process = Runtime.getRuntime().exec("which ngrok");
            return process.waitFor() == 0;
        } catch (Exception e) {
            log.warn("Error checking if ngrok is installed", e);
            return false;
        }
    }
}
