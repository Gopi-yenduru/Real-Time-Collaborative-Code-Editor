package com.codeeditor.service;

import com.codeeditor.model.DocumentUpdate;
import com.codeeditor.repository.DocumentUpdateRepository;
import com.codeeditor.redis.CollaborationRelay;
import com.codeeditor.websocket.Frames;
import com.codeeditor.websocket.YjsMessageType;
import com.codeeditor.websocket.YjsSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * Orchestrates real-time collaboration: persists the CRDT update log and fans
 * frames out to every participant (locally and, via Redis, across nodes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationService {

    private final DocumentUpdateRepository updateRepository;
    private final YjsSessionRegistry registry;
    private final CollaborationRelay relay;

    /** Full ordered history for a document, replayed to a joining client. */
    public List<byte[]> loadUpdates(String documentId) {
        return updateRepository.findByDocumentIdOrderByIdAsc(documentId).stream()
                .map(DocumentUpdate::getPayload)
                .toList();
    }

    /** A client edited the document: persist and broadcast to everyone else. */
    @Transactional
    public void onSync(String documentId, WebSocketSession origin, byte[] update) {
        updateRepository.save(DocumentUpdate.builder()
                .documentId(documentId)
                .payload(update)
                .build());

        byte[] frame = Frames.encode(YjsMessageType.SYNC, update);
        registry.broadcast(documentId, frame, origin.getId());
        relay.publish(documentId, YjsMessageType.SYNC, update);
    }

    /** A client moved its cursor/selection: broadcast only, never persisted. */
    public void onAwareness(String documentId, WebSocketSession origin, byte[] update) {
        byte[] frame = Frames.encode(YjsMessageType.AWARENESS, update);
        registry.broadcast(documentId, frame, origin.getId());
        relay.publish(documentId, YjsMessageType.AWARENESS, update);
    }

    /**
     * Compact the append-only log into a single merged state supplied by a client.
     * <p>
     * Only rows that existed <em>before</em> this snapshot are removed; any update
     * that arrived concurrently (id greater than the pre-snapshot maximum) is kept,
     * so a client editing at the same instant can never be dropped from the log.
     * Re-applying an update already folded into the snapshot is harmless — Yjs
     * updates are idempotent.
     */
    @Transactional
    public void onSnapshot(String documentId, byte[] mergedState) {
        Long priorMaxId = updateRepository.findMaxId(documentId);
        updateRepository.save(DocumentUpdate.builder()
                .documentId(documentId)
                .payload(mergedState)
                .build());
        if (priorMaxId != null) {
            updateRepository.deleteByDocumentIdAndIdLessThanEqual(documentId, priorMaxId);
        }
        log.debug("Compacted update log for document {} (kept rows after id {})", documentId, priorMaxId);
    }

    /**
     * Replace the document's entire state with a snapshot and tell every connected
     * client to discard its copy and re-sync. Used by "restore version".
     */
    @Transactional
    public void resetToState(String documentId, byte[] state) {
        updateRepository.deleteByDocumentId(documentId);
        updateRepository.save(DocumentUpdate.builder()
                .documentId(documentId)
                .payload(state)
                .build());

        byte[] frame = Frames.encode(YjsMessageType.RESET);
        registry.broadcast(documentId, frame, null);
        relay.publish(documentId, YjsMessageType.RESET, new byte[0]);
    }
}
