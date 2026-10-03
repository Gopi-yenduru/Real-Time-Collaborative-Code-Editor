package com.codeeditor.dto;

import com.codeeditor.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Document-related request bodies. */
public final class DocumentRequests {

    private DocumentRequests() {
    }

    public record CreateDocument(
            @NotBlank @Size(max = 255) String title,
            String language
    ) {
    }

    public record ShareDocument(
            @NotBlank @Email String email,
            @NotNull Role role
    ) {
    }

    /** Debounced plain-text mirror of the CRDT document, for previews/search. */
    public record UpdateContent(
            String content
    ) {
    }

    /** A client-computed snapshot: merged CRDT state (base64) + plain text. */
    public record CreateSnapshot(
            @Size(max = 120) String label,
            @NotBlank String state,
            String content
    ) {
    }
}
