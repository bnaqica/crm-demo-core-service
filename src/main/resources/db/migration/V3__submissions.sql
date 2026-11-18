-- Portal submissions routed to internal teams. Team ids are the UMS's; there is no team table here.

-- core-service's copy of each portal user (id = portal sub), kept current from portal-intake events
CREATE TABLE portal_user (
    id         UUID PRIMARY KEY,
    email      VARCHAR(320) NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

-- Tried by ascending position; the first rule matching type and region wins
CREATE TABLE routing_rule (
    id       UUID PRIMARY KEY,
    position INTEGER      NOT NULL,
    type     VARCHAR(100) NOT NULL,
    region   VARCHAR(100) NOT NULL,
    team_id  UUID         NOT NULL
);

CREATE INDEX idx_routing_rule_position ON routing_rule (position);

-- id and version are the portal's; row_version is for optimistic locking
CREATE TABLE submission (
    id               UUID PRIMARY KEY,
    portal_user_id   UUID         NOT NULL REFERENCES portal_user (id),
    version          INTEGER      NOT NULL,
    type             VARCHAR(100) NOT NULL,
    region           VARCHAR(100) NOT NULL,
    team_id          UUID         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    rejection_reason VARCHAR(500),
    received_at      TIMESTAMPTZ  NOT NULL,
    decided_at       TIMESTAMPTZ,
    decided_by       VARCHAR(255),
    row_version      BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_submission_team_status ON submission (team_id, status, received_at);

-- References only; the files stay in the portal's private storage
CREATE TABLE submission_document (
    submission_id UUID          NOT NULL REFERENCES submission (id) ON DELETE CASCADE,
    position      INTEGER       NOT NULL,
    document_id   UUID          NOT NULL,
    storage_key   VARCHAR(1024) NOT NULL,
    content_type  VARCHAR(255)  NOT NULL,
    size          BIGINT        NOT NULL,
    sha256        VARCHAR(64)   NOT NULL,
    PRIMARY KEY (submission_id, position)
);

-- Event ids already handled, written in the same transaction as their effect
CREATE TABLE processed_event (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
