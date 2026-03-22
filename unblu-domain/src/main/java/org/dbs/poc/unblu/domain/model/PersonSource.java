package org.dbs.poc.unblu.domain.model;

/**
 * Source d'une personne dans Unblu.
 */
public enum PersonSource {
    /**
     * Personne enregistrée dans la base d'utilisateurs Unblu (agent).
     */
    USER_DB,
    /** Personne virtuelle (visiteur ou bot), non enregistrée en base. */
    VIRTUAL
}
