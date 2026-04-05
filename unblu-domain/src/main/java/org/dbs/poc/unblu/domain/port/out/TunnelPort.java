package org.dbs.poc.unblu.domain.port.out;

/**
 * Port for managing a public tunnel (e.g. ngrok) exposing local endpoints.
 */
public interface TunnelPort {

    /**
     * Démarre le tunnel public. Retourne {@code true} si le démarrage est réussi.
     *
     * @return {@code true} si le tunnel est actif après l'appel
     */
    boolean start();

    /**
     * Arrête le tunnel public s'il est en cours d'exécution.
     */
    void stop();

    /**
     * Retourne l'URL publique courante du tunnel.
     *
     * @return URL publique du tunnel, ou {@code null} si le tunnel n'est pas actif
     */
    String getPublicUrl();

    /**
     * Retourne l'état courant du tunnel.
     *
     * @return statut du tunnel (running + publicUrl)
     */
    TunnelStatus getStatus();

    /**
     * Statut du tunnel public.
     *
     * @param running   {@code true} si le tunnel est actif
     * @param publicUrl URL publique du tunnel (null si arrêté)
     */
    record TunnelStatus(boolean running, String publicUrl) {}
}
