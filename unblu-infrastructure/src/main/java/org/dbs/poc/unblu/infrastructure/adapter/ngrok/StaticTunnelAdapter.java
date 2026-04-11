package org.dbs.poc.unblu.infrastructure.adapter.ngrok;

import lombok.extern.slf4j.Slf4j;
import org.dbs.poc.unblu.domain.port.out.TunnelPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Implémentation de {@link TunnelPort} sans tunnel ngrok.
 *
 * <p>Active lorsque le profil Spring {@code ngrok} est absent.
 * Les URLs publiques sont lues directement depuis la configuration :
 * <ul>
 *   <li>{@code unblu.webhook.public-url} — URL publique exposant unblu-configuration (webhook + bot)</li>
 *   <li>{@code unblu.bot.public-url} — URL publique exposant livekit directement (optionnel)</li>
 * </ul>
 *
 * <p>Aucun tunnel n'est démarré ni arrêté : {@link #start()} retourne {@code true} immédiatement.
 */
@Slf4j
@Service
@Profile("!ngrok")
public class StaticTunnelAdapter implements TunnelPort {

    @Value("${unblu.webhook.public-url:}")
    private String webhookPublicUrl;

    @Value("${unblu.bot.public-url:}")
    private String botPublicUrl;

    @Override
    public boolean start() {
        log.info("StaticTunnelAdapter: profil ngrok absent, URLs statiques — webhook: {}, bot: {}",
                webhookPublicUrl, botPublicUrl);
        return true;
    }

    @Override
    public void stop() {
        log.info("StaticTunnelAdapter: stop() — aucun tunnel à arrêter");
    }

    @Override
    public String getPublicUrl() {
        return webhookPublicUrl.isBlank() ? null : webhookPublicUrl;
    }

    @Override
    public String getBotPublicUrl() {
        String url = botPublicUrl.isBlank() ? webhookPublicUrl : botPublicUrl;
        return url.isBlank() ? null : url;
    }

    @Override
    public TunnelStatus getStatus() {
        boolean active = !webhookPublicUrl.isBlank();
        return new TunnelStatus(active, getPublicUrl(), getBotPublicUrl());
    }
}
