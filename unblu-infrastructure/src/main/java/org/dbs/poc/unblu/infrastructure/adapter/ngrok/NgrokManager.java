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
 * <p>Démarre deux tunnels ngrok :
 * <ul>
 *   <li>{@code webhook} sur le port de webhook-entrypoint (8083) — accès direct, sans proxy</li>
 *   <li>{@code bot} sur le port de livekit (8082) — accès direct, sans proxy</li>
 * </ul>
 *
 * <p>Requires an ngrok authtoken configured ({@code ngrok config add-authtoken <token>}).
 */
@Slf4j
@Service
@Profile("ngrok")
public class NgrokManager implements TunnelPort {

    @Value("${webhook.entrypoint.local-port:8083}")
    private int webhookLocalPort;

    @Value("${livekit.local-port:8082}")
    private int livekitLocalPort;

    private Process ngrokProcess;
    private String webhookPublicUrl;
    private String botPublicUrl;
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
        if (webhookPublicUrl != null) {
            return webhookPublicUrl;
        }
        fetchPublicUrls();
        return webhookPublicUrl;
    }

    @Override
    public String getBotPublicUrl() {
        if (botPublicUrl != null) {
            return botPublicUrl;
        }
        fetchPublicUrls();
        return botPublicUrl;
    }

    @Override
    public TunnelStatus getStatus() {
        boolean running = isNgrokRunning();
        if (running) {
            fetchPublicUrls();
        }
        return new TunnelStatus(running, webhookPublicUrl, botPublicUrl);
    }

    // -------------------------------------------------------------------------
    // Internal lifecycle
    // -------------------------------------------------------------------------

    public boolean startNgrok() {
        if (isNgrokRunning()) {
            log.info("Ngrok is already running");
            fetchPublicUrls();
            return true;
        }

        if (!isNgrokInstalled()) {
            log.error("Ngrok is not installed. Please install ngrok and configure an authtoken.");
            return false;
        }

        try {
            Path configFile = writeNgrokConfig();
            log.info("Starting ngrok tunnels (webhook-entrypoint:{}, livekit:{})...", webhookLocalPort, livekitLocalPort);

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

            // Poll until both tunnels are ready (up to 15 seconds)
            for (int i = 0; i < 15; i++) {
                Thread.sleep(1000);
                if (!ngrokProcess.isAlive()) {
                    log.error("Ngrok process exited unexpectedly (exit code: {})", ngrokProcess.exitValue());
                    return false;
                }
                fetchPublicUrls();
                if (webhookPublicUrl != null && botPublicUrl != null) {
                    log.info("Ngrok tunnels ready — webhook-entrypoint: {}, livekit: {}", webhookPublicUrl, botPublicUrl);
                    return true;
                }
                log.debug("Waiting for ngrok tunnel... ({}/15)", i + 1);
            }

            log.error("Ngrok started but tunnels not ready after 15 seconds");
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
        webhookPublicUrl = null;
        botPublicUrl = null;
    }

    public boolean isNgrokRunning() {
        if (ngrokProcess != null && ngrokProcess.isAlive()) {
            return true;
        }
        // Also check by reaching the ngrok API
        return fetchPublicUrlsFromApi() != null;
    }

    // -------------------------------------------------------------------------
    // Ngrok config file generation
    // -------------------------------------------------------------------------

    /**
     * Writes a temporary ngrok config file with two tunnels :
     * - {@code webhook} → unblu-configuration (8081) pour les webhooks Unblu
     * - {@code bot}     → livekit (8082) pour les outbound requests bot (accès direct, sans proxy)
     */
    private Path writeNgrokConfig() throws IOException {
        String config = String.format("""
                version: "3"
                tunnels:
                  webhook:
                    addr: %d
                    proto: http
                  bot:
                    addr: %d
                    proto: http
                """, webhookLocalPort, livekitLocalPort);

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
     * Fetches both tunnel URLs from the ngrok local API and updates the cached fields.
     */
    private void fetchPublicUrls() {
        JsonNode tunnels = fetchPublicUrlsFromApi();
        if (tunnels == null) return;

        for (JsonNode tunnel : tunnels) {
            String name = tunnel.path("name").asText("");
            String publicUrl = tunnel.path("public_url").asText("");
            if (!publicUrl.startsWith("https://")) continue;

            if ("webhook".equals(name)) {
                webhookPublicUrl = publicUrl;
                log.debug("Webhook tunnel URL: {}", webhookPublicUrl);
            } else if ("bot".equals(name)) {
                botPublicUrl = publicUrl;
                log.debug("Bot tunnel URL: {}", botPublicUrl);
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
