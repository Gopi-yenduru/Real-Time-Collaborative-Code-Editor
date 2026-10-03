-- ---------------------------------------------------------------------------
-- Reference schema for the Real-Time Collaborative Code Editor.
--
-- This file documents the data model. It is NOT executed at startup
-- (spring.sql.init.mode=never); Hibernate creates/updates the tables from the
-- JPA entities. Keep it in sync with the entities for readers and DBAs.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS documents (
    id         VARCHAR(36) PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    content    LONGTEXT,                       -- plain-text mirror (preview/search)
    owner_id   BIGINT NOT NULL,
    language   VARCHAR(50) DEFAULT 'plaintext',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS document_users (
    document_id VARCHAR(36) NOT NULL,
    user_id     BIGINT NOT NULL,
    role        ENUM('OWNER', 'EDITOR', 'VIEWER') NOT NULL,
    joined_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (document_id, user_id),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)     REFERENCES users(id)     ON DELETE CASCADE
);

-- Append-only log of opaque Yjs (CRDT) updates. Replayed to joining clients and
-- periodically compacted into a single merged update.
CREATE TABLE IF NOT EXISTS document_updates (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    payload     LONGBLOB NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc_updates_doc (document_id, id)
);

-- Named point-in-time versions (history): merged CRDT state + text rendering.
CREATE TABLE IF NOT EXISTS document_snapshots (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    author_id   BIGINT,
    label       VARCHAR(120),
    state       LONGBLOB NOT NULL,
    content     LONGTEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc_snapshots_doc (document_id, id),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id)   REFERENCES users(id)     ON DELETE SET NULL
);
