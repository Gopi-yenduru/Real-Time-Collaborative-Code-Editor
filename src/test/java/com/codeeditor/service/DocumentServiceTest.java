package com.codeeditor.service;

import com.codeeditor.exception.ForbiddenException;
import com.codeeditor.exception.NotFoundException;
import com.codeeditor.model.Document;
import com.codeeditor.model.User;
import com.codeeditor.repository.DocumentRepository;
import com.codeeditor.repository.DocumentSnapshotRepository;
import com.codeeditor.repository.DocumentUpdateRepository;
import com.codeeditor.repository.DocumentUserRepository;
import com.codeeditor.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock DocumentUserRepository documentUserRepository;
    @Mock DocumentUpdateRepository documentUpdateRepository;
    @Mock DocumentSnapshotRepository documentSnapshotRepository;
    @Mock UserRepository userRepository;

    @InjectMocks DocumentService service;

    private static final String DOC = "doc-1";
    private static final Long OWNER = 7L;

    private Document documentOwnedBy(Long ownerId) {
        return Document.builder()
                .id(DOC)
                .title("Test")
                .owner(User.builder().id(ownerId).username("owner").build())
                .build();
    }

    @Test
    void deleteRemovesEveryChildRowBeforeTheDocument() {
        when(documentRepository.findById(DOC)).thenReturn(Optional.of(documentOwnedBy(OWNER)));

        service.delete(DOC, OWNER);

        // Children must go first: the document row is still referenced until they do.
        InOrder order = inOrder(documentSnapshotRepository, documentUpdateRepository,
                documentUserRepository, documentRepository);
        order.verify(documentSnapshotRepository).deleteByDocumentId(DOC);
        order.verify(documentUpdateRepository).deleteByDocumentId(DOC);
        order.verify(documentUserRepository).deleteByDocumentId(DOC);
        order.verify(documentRepository).delete(any(Document.class));
    }

    @Test
    void deleteByNonOwnerIsForbiddenAndTouchesNothing() {
        when(documentRepository.findById(DOC)).thenReturn(Optional.of(documentOwnedBy(OWNER)));

        assertThrows(ForbiddenException.class, () -> service.delete(DOC, 99L));

        verifyNoInteractions(documentSnapshotRepository, documentUpdateRepository);
        verify(documentUserRepository, never()).deleteByDocumentId(anyString());
        verify(documentRepository, never()).delete(any(Document.class));
    }

    @Test
    void deleteOfMissingDocumentThrowsNotFound() {
        when(documentRepository.findById(DOC)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(DOC, OWNER));

        verifyNoInteractions(documentSnapshotRepository, documentUpdateRepository);
    }
}
