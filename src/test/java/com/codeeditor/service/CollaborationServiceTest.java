package com.codeeditor.service;

import com.codeeditor.model.DocumentUpdate;
import com.codeeditor.redis.CollaborationRelay;
import com.codeeditor.repository.DocumentUpdateRepository;
import com.codeeditor.websocket.YjsMessageType;
import com.codeeditor.websocket.YjsSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock DocumentUpdateRepository updateRepository;
    @Mock YjsSessionRegistry registry;
    @Mock CollaborationRelay relay;
    @Mock WebSocketSession session;

    @InjectMocks CollaborationService service;

    private static final String DOC = "doc-1";

    @BeforeEach
    void setUp() {
        lenient().when(session.getId()).thenReturn("session-A");
    }

    @Test
    void onSyncPersistsBroadcastsAndRelays() {
        byte[] update = {1, 2, 3};

        service.onSync(DOC, session, update);

        // persisted exactly one update row with the raw payload
        ArgumentCaptor<DocumentUpdate> saved = ArgumentCaptor.forClass(DocumentUpdate.class);
        verify(updateRepository).save(saved.capture());
        assertArrayEquals(update, saved.getValue().getPayload());
        assertEquals(DOC, saved.getValue().getDocumentId());

        // fanned out to peers, skipping the origin session
        verify(registry).broadcast(eq(DOC), any(byte[].class), eq("session-A"));
        verify(relay).publish(eq(DOC), eq(YjsMessageType.SYNC), eq(update));
    }

    @Test
    void onAwarenessBroadcastsButNeverPersists() {
        service.onAwareness(DOC, session, new byte[]{9});

        verify(registry).broadcast(eq(DOC), any(byte[].class), eq("session-A"));
        verify(relay).publish(eq(DOC), eq(YjsMessageType.AWARENESS), any(byte[].class));
        verifyNoInteractions(updateRepository);
    }

    @Test
    void onSnapshotCompactsButKeepsConcurrentUpdates() {
        byte[] merged = {7, 7, 7};
        when(updateRepository.findMaxId(DOC)).thenReturn(42L);

        service.onSnapshot(DOC, merged);

        // the merged state is stored first...
        ArgumentCaptor<DocumentUpdate> saved = ArgumentCaptor.forClass(DocumentUpdate.class);
        verify(updateRepository).save(saved.capture());
        assertArrayEquals(merged, saved.getValue().getPayload());
        // ...then only pre-snapshot rows are pruned (id <= 42); newer rows survive
        verify(updateRepository).deleteByDocumentIdAndIdLessThanEqual(DOC, 42L);
        verify(updateRepository, never()).deleteByDocumentId(DOC);
        // compaction does not re-broadcast — peers already hold the state
        verifyNoInteractions(relay);
    }

    @Test
    void onSnapshotOnEmptyLogJustStoresState() {
        when(updateRepository.findMaxId(DOC)).thenReturn(null);

        service.onSnapshot(DOC, new byte[]{1});

        verify(updateRepository).save(any(DocumentUpdate.class));
        verify(updateRepository, never()).deleteByDocumentIdAndIdLessThanEqual(anyString(), anyLong());
    }

    @Test
    void resetToStateReplacesLogAndTellsClientsToResync() {
        service.resetToState(DOC, new byte[]{4, 5});

        verify(updateRepository).deleteByDocumentId(DOC);
        verify(updateRepository).save(any(DocumentUpdate.class));
        // every client (no exclusion) is told to reset
        verify(registry).broadcast(eq(DOC), any(byte[].class), isNull());
        verify(relay).publish(eq(DOC), eq(YjsMessageType.RESET), any(byte[].class));
    }
}
