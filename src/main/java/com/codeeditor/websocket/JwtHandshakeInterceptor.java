package com.codeeditor.websocket;

import com.codeeditor.model.Role;
import com.codeeditor.security.JwtUtil;
import com.codeeditor.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * Authenticates and authorizes the collaboration WebSocket during its HTTP
 * handshake. The client connects to {@code /ws/yjs/{documentId}?token=<JWT>}
 * (browsers can't set headers on a WebSocket, so the token travels as a query
 * parameter over TLS). A connection is only upgraded if the token is valid and
 * the user has access to that document; the resolved identity and role are
 * stashed in the session attributes for the handler.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USERNAME = "username";
    public static final String ATTR_EMAIL = "email";
    public static final String ATTR_DOC_ID = "documentId";
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_CAN_EDIT = "canEdit";

    /** The socket is registered at this prefix; the document id is the segment after it. */
    private static final String PATH_PREFIX = "/ws/yjs/";

    private final JwtUtil jwtUtil;
    private final DocumentService documentService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        URI uri = request.getURI();

        String documentId = documentIdFrom(uri.getPath());
        String token = queryParam(uri.getRawQuery(), "token");

        if (documentId == null || token == null || !jwtUtil.validate(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        Optional<Role> role = documentService.roleOf(documentId, userId);
        if (role.isEmpty()) {
            log.debug("User {} denied access to document {}", userId, documentId);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put(ATTR_USER_ID, userId);
        attributes.put(ATTR_USERNAME, jwtUtil.getUsername(token));
        attributes.put(ATTR_EMAIL, jwtUtil.getEmail(token));
        attributes.put(ATTR_DOC_ID, documentId);
        attributes.put(ATTR_ROLE, role.get().name());
        attributes.put(ATTR_CAN_EDIT, role.get() != Role.VIEWER);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    /**
     * Extracts the document id from the path segment following {@code /ws/yjs/}.
     * Returns {@code null} for any path that doesn't carry one, so a request to
     * the bare prefix is rejected outright instead of being read as a document
     * named after a path segment.
     */
    private static String documentIdFrom(String path) {
        if (path == null) {
            return null;
        }
        int prefix = path.indexOf(PATH_PREFIX);
        if (prefix < 0) {
            return null;
        }
        String segment = path.substring(prefix + PATH_PREFIX.length());
        int slash = segment.indexOf('/');
        if (slash >= 0) {
            segment = segment.substring(0, slash);
        }
        return segment.isBlank() ? null : segment;
    }

    private static String queryParam(String rawQuery, String key) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && key.equals(pair.substring(0, eq))) {
                return java.net.URLDecoder.decode(pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
