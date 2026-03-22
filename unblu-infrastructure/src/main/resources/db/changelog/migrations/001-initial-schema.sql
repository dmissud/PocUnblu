--liquibase formatted sql

--changeset dbs:001-initial-schema
--comment: Initial schema - conversation history tables

CREATE TABLE conversation_history
(
    id              BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE,
    ended_at        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ukgw6ddwdipjofkrai09j47ci9s UNIQUE (conversation_id)
);

CREATE TABLE participant_history
(
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT       NOT NULL REFERENCES conversation_history (id) ON DELETE CASCADE,
    person_id       VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255),
    type            VARCHAR(50)  NOT NULL
);

CREATE TABLE conversation_event_history
(
    id                  BIGSERIAL PRIMARY KEY,
    conversation_id     BIGINT      NOT NULL REFERENCES conversation_history (id) ON DELETE CASCADE,
    event_type          VARCHAR(50) NOT NULL,
    event_time          TIMESTAMP WITH TIME ZONE NOT NULL,
    message_text        TEXT,
    sender_person_id    VARCHAR(255),
    sender_display_name VARCHAR(255)
);

--rollback DROP TABLE conversation_event_history;
--rollback DROP TABLE participant_history;
--rollback DROP TABLE conversation_history;
