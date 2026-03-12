package com.codeeditor.controller;

import com.codeeditor.model.Document;
import com.codeeditor.model.Role;
import com.codeeditor.security.UserDetailsImpl;
import com.codeeditor.service.DocumentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<?> getDocuments(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(documentService.getUserDocuments(userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<Document> createDocument(
            @Valid @RequestBody CreateDocumentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        Document document = documentService.createDocument(
                request.getTitle(),
                request.getLanguage(),
                userDetails.getId()
        );
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable String id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareDocument(
            @PathVariable String id,
            @Valid @RequestBody ShareDocumentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        try {
            documentService.shareDocument(id, request.getEmail(), request.getRole(), userDetails.getId());
            return ResponseEntity.ok(Map.of("message", "Document shared successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Data
    public static class CreateDocumentRequest {
        @NotBlank
        private String title;
        private String language;
    }

    @Data
    public static class ShareDocumentRequest {
        @NotBlank
        private String email;
        private Role role;
    }
}
