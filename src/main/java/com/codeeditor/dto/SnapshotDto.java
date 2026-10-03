package com.codeeditor.dto;

import com.codeeditor.model.DocumentSnapshot;

import java.time.Instant;

/** Metadata view of a version-history snapshot (excludes the raw CRDT state). */
public record SnapshotDto(
        Long id,
        String label,
        String authorUsername,
        String content,
        Instant createdAt
) {

    public static SnapshotDto from(DocumentSnapshot s) {
        return new SnapshotDto(
                s.getId(),
                s.getLabel(),
                s.getAuthor() != null ? s.getAuthor().getUsername() : null,
                s.getContent(),
                s.getCreatedAt());
    }
}
