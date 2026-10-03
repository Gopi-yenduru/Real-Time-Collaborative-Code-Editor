package com.codeeditor.websocket;

import com.codeeditor.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.nio.ByteBuffer;

/**
 * Handles the collaboration WebSocket. Identity and document access have already
 * been established by {@link JwtHandshakeInterceptor}; this class only routes
 * binary frames (see {@link YjsMessageType}).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class YjsWebSocketHandler extends BinaryWebSocketHandler {

    private static final int MAX_FRAME_BYTES = 4 * 1024 * 1024; // 4 MB
    private static final int SEND_TIME_LIMIT_MS = 20_000;
    private static final String ATTR_DECORATOR = "concurrentSession";

    private final CollaborationService collaboration;
    private final YjsSessionRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setBinaryMessageSizeLimit(MAX_FRAME_BYTES);
        // Wrap once so every send (fan-out + history stream) is serialized and thread-safe.
        WebSocketSession concurrent =
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, MAX_FRAME_BYTES);
        session.getAttributes().put(ATTR_DECORATOR, concurrent);
        registry.add(documentId(session), concurrent);
        log.debug("WS connected: user={} doc={} session={}",
                session.getAttributes().get(JwtHandshakeInterceptor.ATTR_USERNAME),
                documentId(session), session.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer buffer = message.getPayload();
        if (buffer.remaining() < 1) {
            return;
        }

        String documentId = documentId(session);
        boolean canEdit = Boolean.TRUE.equals(session.getAttributes().get(JwtHandshakeInterceptor.ATTR_CAN_EDIT));
        byte type = Frames.type(buffer);

        switch (type) {
            case YjsMessageType.SYNC_REQUEST -> streamHistory(concurrent(session), documentId);
            case YjsMessageType.SYNC -> {
                if (canEdit) {
                    collaboration.onSync(documentId, session, Frames.payload(buffer));
                }
            }
            case YjsMessageType.AWARENESS ->
                    collaboration.onAwareness(documentId, session, Frames.payload(buffer));
            case YjsMessageType.SNAPSHOT -> {
                if (canEdit) {
                    collaboration.onSnapshot(documentId, Frames.payload(buffer));
                }
            }
            default -> log.debug("Ignoring unknown frame type {} on doc {}", type, documentId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.remove(documentId(session), session.getId());
        log.debug("WS closed: doc={} session={} status={}", documentId(session), session.getId(), status);
    }

    /** Replay the stored update log to a joining client, then mark it synced. */
    private void streamHistory(WebSocketSession session, String documentId) {
        for (byte[] update : collaboration.loadUpdates(documentId)) {
            registry.send(session, Frames.encode(YjsMessageType.SYNC, update));
        }
        registry.send(session, Frames.encode(YjsMessageType.SYNCED));
    }

    private String documentId(WebSocketSession session) {
        return (String) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_DOC_ID);
    }

    private WebSocketSession concurrent(WebSocketSession session) {
        WebSocketSession decorator = (WebSocketSession) session.getAttributes().get(ATTR_DECORATOR);
        return decorator != null ? decorator : session;
    }
}
