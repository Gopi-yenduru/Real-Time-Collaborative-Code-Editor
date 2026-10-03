package com.codeeditor.dto;

import com.codeeditor.model.Document;
import com.codeeditor.model.Role;

import java.time.LocalDateTime;

/**
 * A document as seen by a particular user, including that user's access role.
 * {@code content} is only populated for the single-document (detail) endpoint.
 */
public record DocumentDto(
        String id,
        String title,
        String language,
        String content,
        Long ownerId,
        String ownerUsername,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DocumentDto summary(Document doc, Role role) {
        return new DocumentDto(
                doc.getId(), doc.getTitle(), doc.getLanguage(), null,
                doc.getOwner().getId(), doc.getOwner().getUsername(), role,
                doc.getCreatedAt(), doc.getUpdatedAt());
    }

    public static DocumentDto detail(Document doc, Role role) {
        return new DocumentDto(
                doc.getId(), doc.getTitle(), doc.getLanguage(), doc.getContent(),
                doc.getOwner().getId(), doc.getOwner().getUsername(), role,
                doc.getCreatedAt(), doc.getUpdatedAt());
    }
}
