package com.codeeditor.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * A named point-in-time version of a document (the "history" feature).
 * <p>
 * Stores both the merged Yjs state (for a faithful restore) and a plain-text
 * rendering (for preview/diffing). Snapshots are created by clients, which are
 * the only participants able to compute the merged CRDT state.
 */
@Entity
@Table(name = "document_snapshots", indexes = {
        @Index(name = "idx_doc_snapshots_doc", columnList = "document_id, id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User author;

    @Column(length = 120)
    private String label;

    /** Full merged Yjs state (Y.encodeStateAsUpdate). Used to restore. */
    @Lob
    @Column(name = "state", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] state;

    /** Plain-text rendering at snapshot time, for preview. */
    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
