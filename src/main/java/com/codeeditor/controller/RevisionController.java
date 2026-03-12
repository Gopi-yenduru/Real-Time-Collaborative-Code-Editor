package com.codeeditor.controller;

import com.codeeditor.model.Revision;
import com.codeeditor.service.RevisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/documents/{id}")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionService revisionService;

    @GetMapping("/revisions")
    public ResponseEntity<Page<Revision>> getDocumentRevisions(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return ResponseEntity.ok(revisionService.getDocumentRevisions(id, page, size));
    }

    @PostMapping("/restore/{revisionNumber}")
    public ResponseEntity<?> restoreDocument(
            @PathVariable String id,
            @PathVariable int revisionNumber) {
        
        try {
            revisionService.restoreDocumentToRevision(id, revisionNumber);
            return ResponseEntity.ok(Map.of("message", "Document restored to revision " + revisionNumber));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
