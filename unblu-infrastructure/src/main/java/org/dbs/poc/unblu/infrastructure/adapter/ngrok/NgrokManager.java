package org.dbs.poc.unblu.infrastructure.adapter.ngrok;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.application.port.out.TunnelPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Adapter implementing {@link TunnelPort} using ngrok as the underlying tunnel provider.
 *
 * <p>Manages the full lifecycle of an ngrok process: install check, start, public URL discovery
 * via ngrok's local API ({@code http://localhost:4040/api/tunnels}), and graceful shutdown.
 *
 * <p>Used during local development to expose the webhook callback endpoint ({@code /api/webhooks/unblu})
 * to the Unblu cloud service.
 */
@Slf4j
@Service
public class NgrokManager implements TunnelPort {

    @Value("${unblu.webhook.local-port:8081}")
    private int localPort;

    private Process ngrokProcess;
    private String currentPublicUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Check if ngrok is installed on the system
     */
    public boolean isNgrokInstalled() {
        try {
            Process process = Runtime.getRuntime().exec("which ngrok");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("Error checking if ngrok is installed", e);
            return false;
        }
    }

    /**
     * Install ngrok using snap (requires sudo)
     * Returns true if installation successful
     */
    public boolean installNgrok() {
        log.info("Attempting to install ngrok via snap...");
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "snap", "install", "ngrok");
            pb.inheritIO();
            Process process = pb.start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                log.info("Ngrok installed successfully");
                return true;
            } else {
                log.error("Failed to install ngrok, exit code: {}", process.exitValue());
                return false;
            }
        } catch (Exception e) {
            log.error("Error installing ngrok", e);
            return false;
        }
    }

    /**
     * Start ngrok tunnel on the configured local port
     * Returns true if started successfully
     */
    public boolean startNgrok() {
        if (isNgrokRunning()) {
            log.info("Ngrok is already running");
            return true;
        }

        if (!isNgrokInstalled()) {
            log.warn("Ngrok is not installed, attempting to install...");
            if (!installNgrok()) {
                log.error("Cannot start ngrok: installation failed");
                return false;
            }
        }

        try {
            log.info("Starting ngrok tunnel on port {}...", localPort);
            ProcessBuilder pb = new ProcessBuilder("ngrok", "http", String.valueOf(localPort));
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            ngrokProcess = pb.start();

            // Poll ngrok API until tunnel is ready (up to 10 seconds)
            String url = null;
            for (int i = 0; i < 10; i++) {
                Thread.sleep(1000);
                if (!ngrokProcess.isAlive()) {
                    log.error("Ngrok process exited unexpectedly (exit code: {})", ngrokProcess.exitValue());
                    return false;
                }
                url = fetchPublicUrl();
                if (url != null) break;
                log.debug("Waiting for ngrok tunnel... ({}/10)", i + 1);
            }

            if (url != null) {
                currentPublicUrl = url;
                log.info("Ngrok tunnel started successfully: {}", currentPublicUrl);
                return true;
            } else {
                log.error("Ngrok started but failed to get public URL after 10 seconds");
                stopNgrok();
                return false;
            }
        } catch (Exception e) {
            log.error("Error starting ngrok", e);
            stopNgrok();
            return false;
        }
    }

    /**
     * Get the current public URL from ngrok API
     */
    public String getPublicUrl() {
        if (currentPublicUrl != null) {
            return currentPublicUrl;
        }

        currentPublicUrl = fetchPublicUrl();
        return currentPublicUrl;
    }

    /**
     * Fetch public URL from ngrok's local API
     */
    private String fetchPublicUrl() {
        try {
            URL url = new URL("http://localhost:4040/api/tunnels");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse JSON response
                JsonNode root = objectMapper.readTree(response.toString());
                JsonNode tunnels = root.get("tunnels");

                if (tunnels != null && tunnels.isArray() && tunnels.size() > 0) {
                    // Get the first HTTPS tunnel
                    for (JsonNode tunnel : tunnels) {
                        String publicUrl = tunnel.get("public_url").asText();
                        if (publicUrl.startsWith("https://")) {
                            return publicUrl;
                        }
                    }
                }
            } else {
                log.warn("Ngrok API returned status code: {}", responseCode);
            }
        } catch (IOException e) {
            log.debug("Could not fetch ngrok public URL (ngrok may not be running)", e);
        }
        return null;
    }

    /**
     * Check if ngrok is currently running
     */
    public boolean isNgrokRunning() {
        // Check if we have a process reference and it's alive
        if (ngrokProcess != null && ngrokProcess.isAlive()) {
            return true;
        }

        // Also check by trying to reach ngrok API
        String url = fetchPublicUrl();
        return url != null;
    }

    /**
     * Stop the ngrok tunnel
     */
    public void stopNgrok() {
        if (ngrokProcess != null && ngrokProcess.isAlive()) {
            log.info("Stopping ngrok tunnel...");
            ngrokProcess.destroy();
            try {
                ngrokProcess.waitFor(5, TimeUnit.SECONDS);
                if (ngrokProcess.isAlive()) {
                    log.warn("Ngrok didn't stop gracefully, forcing kill...");
                    ngrokProcess.destroyForcibly();
                }
                log.info("Ngrok tunnel stopped");
            } catch (InterruptedException e) {
                log.error("Error waiting for ngrok to stop", e);
                Thread.currentThread().interrupt();
            }
        }
        ngrokProcess = null;
        currentPublicUrl = null;
    }

    @Override
    public boolean start() {
        return startNgrok();
    }

    @Override
    public void stop() {
        stopNgrok();
    }

    /**
     * Get ngrok status information
     */
    @Override
    public TunnelStatus getStatus() {
        boolean running = isNgrokRunning();
        String url = running ? getPublicUrl() : null;
        return new TunnelStatus(running, url);
    }
}
