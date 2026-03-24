--liquibase formatted sql

--changeset dbs:003-add-message-id-to-event-history
--comment: Ajout de la colonne message_id pour la déduplication des messages lors du fetch Unblu

ALTER TABLE conversation_event_history
    ADD COLUMN message_id VARCHAR(255);

CREATE INDEX idx_event_history_message_id ON conversation_event_history (message_id)
    WHERE message_id IS NOT NULL;

--rollback DROP INDEX idx_event_history_message_id;
--rollback ALTER TABLE conversation_event_history DROP COLUMN message_id;
