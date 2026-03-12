package com.codeeditor.service;

import com.codeeditor.engine.OTEngine;
import com.codeeditor.engine.Operation;
import com.codeeditor.model.Document;
import com.codeeditor.model.DocumentUser;
import com.codeeditor.model.DocumentUserId;
import com.codeeditor.model.Role;
import com.codeeditor.model.User;
import com.codeeditor.repository.DocumentRepository;
import com.codeeditor.repository.DocumentUserRepository;
import com.codeeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentUserRepository documentUserRepository;
    private final UserRepository userRepository;
    private final OTEngine otEngine;

    @Transactional
    public Document createDocument(String title, String language, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Document document = Document.builder()
                .title(title)
                .language(language != null ? language : "plaintext")
                .content("")
                .owner(owner)
                .build();

        document = documentRepository.save(document);

        // Add owner to document_users
        DocumentUser documentUser = DocumentUser.builder()
                .id(new DocumentUserId(document.getId(), ownerId))
                .document(document)
                .user(owner)
                .role(Role.OWNER)
                .build();

        documentUserRepository.save(documentUser);

        return document;
    }

    public Document getDocument(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
    }
    
    public List<DocumentUser> getUserDocuments(Long userId) {
        return documentUserRepository.findByUserId(userId);
    }

    @Transactional
    public void shareDocument(String documentId, String email, Role role, Long requesterId) {
        Document document = getDocument(documentId);

        // Check if requester is OWNER
        if (!document.getOwner().getId().equals(requesterId)) {
            throw new IllegalStateException("Only the owner can share this document");
        }

        User targetUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email not found"));

        DocumentUser documentUser = DocumentUser.builder()
                .id(new DocumentUserId(document.getId(), targetUser.getId()))
                .document(document)
                .user(targetUser)
                .role(role)
                .build();

        documentUserRepository.save(documentUser);
    }

    @Transactional
    public void applyOperationToDocument(String documentId, Operation operation) {
        Document document = getDocument(documentId);
        String currentContent = document.getContent();
        
        String newContent = otEngine.apply(currentContent, operation);
        document.setContent(newContent);
        
        documentRepository.save(document);
    }
    
    @Transactional
    public void updateDocumentContent(String documentId, String newContent) {
        Document document = getDocument(documentId);
        document.setContent(newContent);
        documentRepository.save(document);
    }
}
