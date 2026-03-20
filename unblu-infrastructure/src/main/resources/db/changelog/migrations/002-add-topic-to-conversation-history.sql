--liquibase formatted sql

--changeset dbs:002-add-topic-to-conversation-history
--comment: Add topic column to store conversation title received from Unblu webhook

ALTER TABLE conversation_history
    ADD COLUMN topic VARCHAR(500);

--rollback ALTER TABLE conversation_history DROP COLUMN topic;
