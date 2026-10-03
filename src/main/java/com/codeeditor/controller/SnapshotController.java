package com.codeeditor.controller;

import com.codeeditor.dto.DocumentRequests.CreateSnapshot;
import com.codeeditor.dto.SnapshotDto;
import com.codeeditor.security.UserDetailsImpl;
import com.codeeditor.service.SnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/documents/{id}/snapshots")
@RequiredArgsConstructor
@Tag(name = "Version history", description = "Save and restore named document versions")
public class SnapshotController {

    private final SnapshotService snapshotService;

    @GetMapping
    @Operation(summary = "List saved versions (most recent first)")
    public Page<SnapshotDto> list(@PathVariable String id,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @AuthenticationPrincipal UserDetailsImpl user) {
        return snapshotService.list(id, user.getId(), page, size);
    }

    @PostMapping
    @Operation(summary = "Save the current document as a new version")
    public ResponseEntity<SnapshotDto> create(@PathVariable String id,
                                              @Valid @RequestBody CreateSnapshot request,
                                              @AuthenticationPrincipal UserDetailsImpl user) {
        SnapshotDto dto = snapshotService.create(id, user.getId(), request.label(), request.state(), request.content());
        return ResponseEntity.status(201).body(dto);
    }

    @PostMapping("/{snapshotId}/restore")
    @Operation(summary = "Restore the document to a saved version (resets all clients)")
    public ResponseEntity<Map<String, String>> restore(@PathVariable String id,
                                                        @PathVariable Long snapshotId,
                                                        @AuthenticationPrincipal UserDetailsImpl user) {
        snapshotService.restore(id, user.getId(), snapshotId);
        return ResponseEntity.ok(Map.of("message", "Document restored"));
    }
}
