package org.dbs.poc.unblu.application.port.out;

/**
 * Port for managing a public tunnel (e.g. ngrok) exposing local endpoints.
 */
public interface TunnelPort {

    boolean start();

    void stop();

    String getPublicUrl();

    TunnelStatus getStatus();

    record TunnelStatus(boolean running, String publicUrl) {}
}
