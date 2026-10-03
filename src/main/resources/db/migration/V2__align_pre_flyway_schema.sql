-- ---------------------------------------------------------------------------
-- V2 — bring databases created before Flyway in line with V1.
--
-- Those schemas were built partly by the old reference schema.sql and partly by
-- Hibernate ddl-auto=update, which left two differences:
--
--   * a dead `revisions` table from the Operational Transformation era, which
--     nothing has referenced since the CRDT rewrite;
--   * document_snapshots foreign keys created with NO ACTION, so deleting a
--     document with saved versions was rejected by the database.
--
-- Every statement is conditional, because this migration also runs on a fresh
-- database where V1 has already produced the correct shape. MySQL has no
-- DROP FOREIGN KEY IF EXISTS, hence the information_schema lookups and
-- prepared statements; `DO 0` is the no-op branch.
-- ---------------------------------------------------------------------------

-- Checked rather than DROP ... IF EXISTS, which logs a warning on every fresh
-- database that never had the table.
SET @has_revisions := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'revisions');

SET @sql := IF(@has_revisions = 0, 'DO 0', 'DROP TABLE revisions');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- --- document_snapshots.document_id -> documents.id (ON DELETE CASCADE) -----

SET @legacy_fk := (
    SELECT rc.CONSTRAINT_NAME
    FROM information_schema.REFERENTIAL_CONSTRAINTS rc
    JOIN information_schema.KEY_COLUMN_USAGE kcu
      ON kcu.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA
     AND kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME
    WHERE rc.CONSTRAINT_SCHEMA = DATABASE()
      AND rc.TABLE_NAME = 'document_snapshots'
      AND kcu.COLUMN_NAME = 'document_id'
      AND rc.DELETE_RULE <> 'CASCADE'
    LIMIT 1);

SET @sql := IF(@legacy_fk IS NULL, 'DO 0',
    CONCAT('ALTER TABLE document_snapshots DROP FOREIGN KEY ', @legacy_fk));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk := (
    SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'fk_document_snapshots_document');

SET @sql := IF(@has_fk > 0, 'DO 0',
    'ALTER TABLE document_snapshots ADD CONSTRAINT fk_document_snapshots_document '
    'FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- --- document_snapshots.author_id -> users.id (ON DELETE SET NULL) ----------

SET @legacy_author_fk := (
    SELECT rc.CONSTRAINT_NAME
    FROM information_schema.REFERENTIAL_CONSTRAINTS rc
    JOIN information_schema.KEY_COLUMN_USAGE kcu
      ON kcu.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA
     AND kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME
    WHERE rc.CONSTRAINT_SCHEMA = DATABASE()
      AND rc.TABLE_NAME = 'document_snapshots'
      AND kcu.COLUMN_NAME = 'author_id'
      AND rc.DELETE_RULE <> 'SET NULL'
    LIMIT 1);

SET @sql := IF(@legacy_author_fk IS NULL, 'DO 0',
    CONCAT('ALTER TABLE document_snapshots DROP FOREIGN KEY ', @legacy_author_fk));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_author_fk := (
    SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'fk_document_snapshots_author');

SET @sql := IF(@has_author_fk > 0, 'DO 0',
    'ALTER TABLE document_snapshots ADD CONSTRAINT fk_document_snapshots_author '
    'FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
