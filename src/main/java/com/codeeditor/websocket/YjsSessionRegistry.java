package com.codeeditor.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the WebSocket sessions connected to each document on <em>this</em> node
 * and fans binary frames out to them. Sessions are keyed by id (not object
 * identity) so the thread-safe decorator registered on connect is the exact
 * object removed on close. Cross-node delivery is layered on top by
 * {@link com.codeeditor.redis.CollaborationRelay}.
 */
@Component
@Slf4j
public class YjsSessionRegistry {

    /** documentId &rarr; (sessionId &rarr; session) for sessions live on this node. */
    private final Map<String, Map<String, WebSocketSession>> sessionsByDoc = new ConcurrentHashMap<>();

    public void add(String documentId, WebSocketSession session) {
        sessionsByDoc.computeIfAbsent(documentId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
    }

    public void remove(String documentId, String sessionId) {
        sessionsByDoc.computeIfPresent(documentId, (id, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public int countForDoc(String documentId) {
        Map<String, WebSocketSession> sessions = sessionsByDoc.get(documentId);
        return sessions == null ? 0 : sessions.size();
    }

    /**
     * Sends a frame to every local session on a document, optionally skipping the
     * session it originated from.
     *
     * @param exceptSessionId session id to skip, or {@code null} to send to all
     */
    public void broadcast(String documentId, byte[] frame, String exceptSessionId) {
        Map<String, WebSocketSession> sessions = sessionsByDoc.get(documentId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (exceptSessionId != null && exceptSessionId.equals(entry.getKey())) {
                continue;
            }
            send(entry.getValue(), frame);
        }
    }

    public void send(WebSocketSession session, byte[] frame) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new BinaryMessage(ByteBuffer.wrap(frame)));
        } catch (IOException e) {
            log.debug("Failed to send to session {}: {}", session.getId(), e.getMessage());
            try {
                session.close();
            } catch (IOException ignored) {
                // already closing
            }
        }
    }
}
