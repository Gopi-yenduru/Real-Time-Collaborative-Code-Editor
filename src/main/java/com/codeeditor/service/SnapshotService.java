package com.codeeditor.service;

import com.codeeditor.dto.SnapshotDto;
import com.codeeditor.exception.NotFoundException;
import com.codeeditor.model.Document;
import com.codeeditor.model.DocumentSnapshot;
import com.codeeditor.model.User;
import com.codeeditor.repository.DocumentSnapshotRepository;
import com.codeeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

/**
 * Version history for documents. A snapshot bundles the merged CRDT state
 * (computed by the client) with a plain-text rendering, so it can be both
 * previewed and faithfully restored.
 */
@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final DocumentSnapshotRepository snapshotRepository;
    private final DocumentService documentService;
    private final CollaborationService collaborationService;
    private final UserRepository userRepository;

    @Transactional
    public SnapshotDto create(String documentId, Long userId, String label, String stateBase64, String content) {
        documentService.requireEditAccess(documentId, userId);
        Document document = documentService.getEntity(documentId);
        User author = userRepository.getReferenceById(userId);

        byte[] state = Base64.getDecoder().decode(stateBase64);

        DocumentSnapshot snapshot = snapshotRepository.save(DocumentSnapshot.builder()
                .document(document)
                .author(author)
                .label(label != null && !label.isBlank() ? label : "Version")
                .state(state)
                .content(content)
                .build());

        return SnapshotDto.from(snapshot);
    }

    @Transactional(readOnly = true)
    public Page<SnapshotDto> list(String documentId, Long userId, int page, int size) {
        documentService.requireAccess(documentId, userId);
        return snapshotRepository
                .findByDocumentIdOrderByIdDesc(documentId, PageRequest.of(page, Math.min(size, 100)))
                .map(SnapshotDto::from);
    }

    /**
     * Restore a document to a snapshot: reset the CRDT state for every connected
     * client and refresh the plain-text mirror.
     */
    @Transactional
    public void restore(String documentId, Long userId, Long snapshotId) {
        documentService.requireEditAccess(documentId, userId);

        DocumentSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new NotFoundException("Snapshot not found"));
        if (!snapshot.getDocument().getId().equals(documentId)) {
            throw new NotFoundException("Snapshot does not belong to this document");
        }

        collaborationService.resetToState(documentId, snapshot.getState());
        documentService.updateContent(documentId, userId, snapshot.getContent());
    }
}
