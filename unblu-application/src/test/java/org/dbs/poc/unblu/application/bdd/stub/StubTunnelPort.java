package org.dbs.poc.unblu.application.bdd.stub;

import org.dbs.poc.unblu.domain.port.out.TunnelPort;
import org.springframework.stereotype.Component;

/**
 * Stub de {@link TunnelPort} — tunnel toujours inactif en test.
 */
@Component
public class StubTunnelPort implements TunnelPort {

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public void stop() {
    }

    @Override
    public String getPublicUrl() {
        return null;
    }

    @Override
    public TunnelStatus getStatus() {
        return new TunnelStatus(false, null);
    }
}
