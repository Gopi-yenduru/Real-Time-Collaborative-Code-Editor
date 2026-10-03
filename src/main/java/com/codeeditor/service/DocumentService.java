package com.codeeditor.service;

import com.codeeditor.dto.DocumentDto;
import com.codeeditor.exception.ForbiddenException;
import com.codeeditor.exception.NotFoundException;
import com.codeeditor.model.*;
import com.codeeditor.repository.DocumentRepository;
import com.codeeditor.repository.DocumentSnapshotRepository;
import com.codeeditor.repository.DocumentUpdateRepository;
import com.codeeditor.repository.DocumentUserRepository;
import com.codeeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentUserRepository documentUserRepository;
    private final DocumentUpdateRepository documentUpdateRepository;
    private final DocumentSnapshotRepository documentSnapshotRepository;
    private final UserRepository userRepository;

    // ----- Reads -------------------------------------------------------------

    public Document getEntity(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
    }

    @Transactional(readOnly = true)
    public Optional<Role> roleOf(String documentId, Long userId) {
        return documentUserRepository.findByIdDocumentIdAndIdUserId(documentId, userId)
                .map(DocumentUser::getRole);
    }

    /** Returns the caller's role, or throws 403 if they have no access. */
    public Role requireAccess(String documentId, Long userId) {
        return roleOf(documentId, userId)
                .orElseThrow(() -> new ForbiddenException("You do not have access to this document"));
    }

    /** Ensures the caller may edit (OWNER or EDITOR), else throws 403. */
    public Role requireEditAccess(String documentId, Long userId) {
        Role role = requireAccess(documentId, userId);
        if (role == Role.VIEWER) {
            throw new ForbiddenException("You have read-only access to this document");
        }
        return role;
    }

    public boolean canEdit(String documentId, Long userId) {
        return roleOf(documentId, userId).map(r -> r != Role.VIEWER).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> listForUser(Long userId) {
        return documentUserRepository.findByIdUserId(userId).stream()
                .map(du -> DocumentDto.summary(du.getDocument(), du.getRole()))
                .sorted(Comparator.comparing(DocumentDto::updatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDto getForUser(String documentId, Long userId) {
        Role role = requireAccess(documentId, userId);
        return DocumentDto.detail(getEntity(documentId), role);
    }

    // ----- Writes ------------------------------------------------------------

    @Transactional
    public DocumentDto create(String title, String language, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Document document = documentRepository.save(Document.builder()
                .title(title)
                .language(language != null && !language.isBlank() ? language : "plaintext")
                .content("")
                .owner(owner)
                .build());

        documentUserRepository.save(DocumentUser.builder()
                .id(new DocumentUserId(document.getId(), ownerId))
                .document(document)
                .user(owner)
                .role(Role.OWNER)
                .build());

        return DocumentDto.detail(document, Role.OWNER);
    }

    @Transactional
    public void share(String documentId, String email, Role role, Long requesterId) {
        Document document = getEntity(documentId);

        if (!document.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenException("Only the owner can share this document");
        }
        if (role == null || role == Role.OWNER) {
            throw new IllegalArgumentException("Role must be EDITOR or VIEWER");
        }

        User target = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No user found with email " + email));

        documentUserRepository.save(DocumentUser.builder()
                .id(new DocumentUserId(documentId, target.getId()))
                .document(document)
                .user(target)
                .role(role)
                .build());
    }

    /** Persists the debounced plain-text mirror used for previews/search. */
    @Transactional
    public void updateContent(String documentId, Long userId, String content) {
        requireEditAccess(documentId, userId);
        Document document = getEntity(documentId);
        document.setContent(content);
        documentRepository.save(document);
    }

    /**
     * Deletes a document along with everything hanging off it: the CRDT update
     * log, version-history snapshots, and membership rows.
     * <p>
     * The children are removed explicitly rather than left to database cascades.
     * {@code document_updates} has no foreign key at all (the CRDT log is
     * deliberately decoupled from the entity model), and {@code ddl-auto=update}
     * never rewrites a constraint that already exists — so a schema created before
     * the cascades were declared would still reject the delete. Doing it here
     * works on any schema.
     */
    @Transactional
    public void delete(String documentId, Long userId) {
        Document document = getEntity(documentId);
        if (!document.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the owner can delete this document");
        }
        documentSnapshotRepository.deleteByDocumentId(documentId);
        documentUpdateRepository.deleteByDocumentId(documentId);
        documentUserRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
    }
}
