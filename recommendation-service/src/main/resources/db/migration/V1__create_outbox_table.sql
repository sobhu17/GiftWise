-- V1__create_outbox_table.sql
-- Transactional outbox for recommendation-service.
-- A row here is written in the SAME DB transaction as the recommendation
-- result it describes, so "recommendation saved" and "event durably recorded"
-- can never disagree — the outbox relay (a separate @Scheduled process) polls
-- PENDING rows and publishes them to Kafka independently, so a Kafka outage
-- never blocks or loses a recommendation response to the user.

CREATE TABLE outbox_events (
                               id              UUID            NOT NULL DEFAULT gen_random_uuid(),
                               event_type      VARCHAR(255)    NOT NULL,
                               payload         TEXT            NOT NULL,
                               -- Identifies which recommendation this event is about — used as the
                               -- Kafka partition key so events for the same aggregate stay ordered.
                               -- Deliberately NOT a foreign key: the outbox's event log must stay
                               -- decoupled from the source row's lifecycle (deleting/archiving a
                               -- recommendation later should never block or cascade into its events).
                               aggregate_id    UUID            NOT NULL,
                               status          VARCHAR(255)    NOT NULL DEFAULT 'PENDING',
                               -- Nullable: only populated once a publish attempt actually fails.
                               error_message   TEXT            ,
                               retry_count     INTEGER         NOT NULL DEFAULT 0,
                               created_at      TIMESTAMP       NOT NULL DEFAULT now(),
                               -- Nullable: only populated once the relay successfully publishes this row.
                               published_at    TIMESTAMP       ,

                               CONSTRAINT pk_outbox_event PRIMARY KEY (id),
                               CONSTRAINT chk_outbox_event_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

-- Partial index: the relay only ever queries PENDING rows, and this table
-- accumulates PUBLISHED history indefinitely. Indexing only the PENDING
-- subset keeps the index small regardless of total table size, unlike a
-- full index on status which would grow with every historical row.
CREATE INDEX idx_outbox_event_pending ON outbox_events (created_at)
    WHERE status = 'PENDING';