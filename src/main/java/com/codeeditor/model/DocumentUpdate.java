package com.codeeditor.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * An append-only log of Yjs (CRDT) document updates.
 * <p>
 * The server never interprets these bytes — it stores them and replays them to
 * joining clients. Because Yjs updates are commutative and idempotent, replaying
 * the full log (in any order) reconstructs the exact document state. The log is
 * periodically compacted into a single merged update by {@code MSG_SNAPSHOT}.
 */
@Entity
@Table(name = "document_updates", indexes = {
        @Index(name = "idx_doc_updates_doc", columnList = "document_id, id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", length = 36, nullable = false)
    private String documentId;

    /** Raw Yjs update (v1 binary encoding). Opaque to the server. */
    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] payload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
