-- ---------------------------------------------------------------------------
-- V1 — canonical schema.
--
-- Generated from the JPA entity model (Hibernate schema export) and then given
-- explicit constraint names, so every environment ends up with the same
-- identifiers instead of Hibernate's hash-derived ones.
--
-- Databases that predate Flyway are baselined at this version and never run it
-- (spring.flyway.baseline-on-migrate); V2 brings those up to date.
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE documents (
    id         VARCHAR(36)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content    LONGTEXT,
    owner_id   BIGINT       NOT NULL,
    language   VARCHAR(50),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE document_users (
    document_id VARCHAR(36) NOT NULL,
    user_id     BIGINT      NOT NULL,
    role        ENUM ('EDITOR','OWNER','VIEWER') NOT NULL,
    joined_at   DATETIME(6),
    PRIMARY KEY (user_id, document_id),
    CONSTRAINT fk_document_users_document FOREIGN KEY (document_id)
        REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_document_users_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Append-only log of opaque Yjs (CRDT) updates. Deliberately carries no foreign
-- key: the server treats these bytes as opaque and the application removes the
-- rows itself, so the log stays decoupled from the entity model.
CREATE TABLE document_updates (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    document_id VARCHAR(36) NOT NULL,
    payload     LONGBLOB    NOT NULL,
    created_at  DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_doc_updates_doc (document_id, id)
) ENGINE = InnoDB;

-- Named point-in-time versions: merged CRDT state plus a text rendering.
CREATE TABLE document_snapshots (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    document_id VARCHAR(36) NOT NULL,
    author_id   BIGINT,
    label       VARCHAR(120),
    state       LONGBLOB    NOT NULL,
    content     LONGTEXT,
    created_at  DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_doc_snapshots_doc (document_id, id),
    CONSTRAINT fk_document_snapshots_document FOREIGN KEY (document_id)
        REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_document_snapshots_author FOREIGN KEY (author_id)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;
