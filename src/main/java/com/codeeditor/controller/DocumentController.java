package com.codeeditor.controller;

import com.codeeditor.dto.DocumentDto;
import com.codeeditor.dto.DocumentRequests.CreateDocument;
import com.codeeditor.dto.DocumentRequests.ShareDocument;
import com.codeeditor.dto.DocumentRequests.UpdateContent;
import com.codeeditor.security.UserDetailsImpl;
import com.codeeditor.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Create, list, share, and mirror collaborative documents")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "List documents the current user can access")
    public List<DocumentDto> list(@AuthenticationPrincipal UserDetailsImpl user) {
        return documentService.listForUser(user.getId());
    }

    @PostMapping
    @Operation(summary = "Create a new document (caller becomes owner)")
    public ResponseEntity<DocumentDto> create(@Valid @RequestBody CreateDocument request,
                                              @AuthenticationPrincipal UserDetailsImpl user) {
        DocumentDto dto = documentService.create(request.title(), request.language(), user.getId());
        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single document (access-checked, includes content)")
    public DocumentDto get(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl user) {
        return documentService.getForUser(id, user.getId());
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Grant another user EDITOR or VIEWER access (owner only)")
    public ResponseEntity<Map<String, String>> share(@PathVariable String id,
                                                      @Valid @RequestBody ShareDocument request,
                                                      @AuthenticationPrincipal UserDetailsImpl user) {
        documentService.share(id, request.email(), request.role(), user.getId());
        return ResponseEntity.ok(Map.of("message", "Document shared with " + request.email()));
    }

    @PutMapping("/{id}/content")
    @Operation(summary = "Update the plain-text mirror of a document (editors only)")
    public ResponseEntity<Void> updateContent(@PathVariable String id,
                                              @Valid @RequestBody UpdateContent request,
                                              @AuthenticationPrincipal UserDetailsImpl user) {
        documentService.updateContent(id, user.getId(), request.content());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document (owner only)")
    public ResponseEntity<Void> delete(@PathVariable String id, @AuthenticationPrincipal UserDetailsImpl user) {
        documentService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
