package com.codeeditor.service;

import com.codeeditor.engine.OTEngine;
import com.codeeditor.engine.Operation;
import com.codeeditor.model.Document;
import com.codeeditor.model.Revision;
import com.codeeditor.model.User;
import com.codeeditor.repository.DocumentRepository;
import com.codeeditor.repository.RevisionRepository;
import com.codeeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevisionService {

    private final RevisionRepository revisionRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentService documentService;
    private final OTEngine otEngine;

    public int getLatestRevisionNumber(String documentId) {
        return revisionRepository.findMaxRevisionNumberByDocumentId(documentId).orElse(0);
    }

    public List<Operation> getOperationsAfterRevision(String documentId, int revisionNumber) {
        List<Revision> revisions = revisionRepository
                .findByDocumentIdAndRevisionNumberGreaterThanOrderByRevisionNumberAsc(documentId, revisionNumber);

        return revisions.stream().map(r -> Operation.builder()
                .type(r.getOpType())
                .position(r.getPosition())
                .character(r.getCharacter())
                .revision(r.getRevisionNumber())
                .userId(r.getUser().getId())
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public void saveRevision(String documentId, Operation operation) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        User user = userRepository.findById(operation.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Revision revision = Revision.builder()
                .document(document)
                .user(user)
                .opType(operation.getType())
                .position(operation.getPosition())
                .character(operation.getCharacter())
                .revisionNumber(operation.getRevision())
                .build();

        revisionRepository.save(revision);
    }

    public Page<Revision> getDocumentRevisions(String documentId, int page, int size) {
        return revisionRepository.findByDocumentIdOrderByRevisionNumberDesc(documentId, PageRequest.of(page, size));
    }

    @Transactional
    public void restoreDocumentToRevision(String documentId, int targetRevisionNumber) {
         Document document = documentRepository.findById(documentId)
                 .orElseThrow(() -> new IllegalArgumentException("Document not found"));
                 
         List<Revision> allRevisions = revisionRepository.findAllByDocumentIdOrderByRevisionNumberAsc(documentId);
         
         String text = "";
         for (Revision rev : allRevisions) {
             if (rev.getRevisionNumber() > targetRevisionNumber) {
                 break;
             }
             Operation op = Operation.builder()
                .type(rev.getOpType())
                .position(rev.getPosition())
                .character(rev.getCharacter())
                .revision(rev.getRevisionNumber())
                .userId(rev.getUser().getId())
                .build();
             text = otEngine.apply(text, op);
         }
         
         documentService.updateDocumentContent(documentId, text);
         
         // In a real robust system, we would either truncate revisions past target, 
         // OR we append an inversion operation to get back to the state.
         // For simplicity, we just updated the text.
         log.info("Document {} restored to revision {}", documentId, targetRevisionNumber);
    }
}
