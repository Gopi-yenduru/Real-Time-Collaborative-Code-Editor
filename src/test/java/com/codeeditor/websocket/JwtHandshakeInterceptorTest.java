package com.codeeditor.websocket;

import com.codeeditor.model.Role;
import com.codeeditor.security.JwtUtil;
import com.codeeditor.security.UserDetailsImpl;
import com.codeeditor.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * The handshake is the only place document access is checked for the
 * collaboration socket, so these cases describe its entire access model.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtHandshakeInterceptorTest {

    private static final String DOC = "doc-1";
    private static final String SECRET = "handshake-test-secret-long-enough-for-hs256";

    @Mock ServerHttpRequest request;
    @Mock ServerHttpResponse response;
    @Mock DocumentService documentService;

    private JwtUtil jwtUtil;
    private JwtHandshakeInterceptor interceptor;
    private Map<String, Object> attributes;
    private String validToken;

    private static JwtUtil issuer(long expirationMs) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(util, "jwtExpirationMs", expirationMs);
        return util;
    }

    @BeforeEach
    void setUp() {
        jwtUtil = issuer(60_000L);
        interceptor = new JwtHandshakeInterceptor(jwtUtil, documentService);
        attributes = new HashMap<>();
        validToken = jwtUtil.generateToken(new UserDetailsImpl(42L, "alice", "alice@example.com", null));
    }

    private boolean handshake(String uri) {
        when(request.getURI()).thenReturn(URI.create(uri));
        return interceptor.beforeHandshake(request, response, null, attributes);
    }

    @Test
    void editorIsAdmittedAndIdentityIsStashedForTheHandler() {
        when(documentService.roleOf(DOC, 42L)).thenReturn(Optional.of(Role.EDITOR));

        assertTrue(handshake("ws://localhost:8080/ws/yjs/" + DOC + "?token=" + validToken));

        assertEquals(42L, attributes.get(JwtHandshakeInterceptor.ATTR_USER_ID));
        assertEquals("alice", attributes.get(JwtHandshakeInterceptor.ATTR_USERNAME));
        assertEquals("alice@example.com", attributes.get(JwtHandshakeInterceptor.ATTR_EMAIL));
        assertEquals(DOC, attributes.get(JwtHandshakeInterceptor.ATTR_DOC_ID));
        assertEquals("EDITOR", attributes.get(JwtHandshakeInterceptor.ATTR_ROLE));
        assertEquals(Boolean.TRUE, attributes.get(JwtHandshakeInterceptor.ATTR_CAN_EDIT));
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void ownerCanEdit() {
        when(documentService.roleOf(DOC, 42L)).thenReturn(Optional.of(Role.OWNER));

        assertTrue(handshake("ws://localhost:8080/ws/yjs/" + DOC + "?token=" + validToken));

        assertEquals(Boolean.TRUE, attributes.get(JwtHandshakeInterceptor.ATTR_CAN_EDIT));
    }

    @Test
    void viewerIsAdmittedButFlaggedReadOnly() {
        when(documentService.roleOf(DOC, 42L)).thenReturn(Optional.of(Role.VIEWER));

        assertTrue(handshake("ws://localhost:8080/ws/yjs/" + DOC + "?token=" + validToken));

        // Admitted so remote cursors still render, but the handler drops its edits.
        assertEquals(Boolean.FALSE, attributes.get(JwtHandshakeInterceptor.ATTR_CAN_EDIT));
        assertEquals("VIEWER", attributes.get(JwtHandshakeInterceptor.ATTR_ROLE));
    }

    @Test
    void userWithNoAccessToTheDocumentIsForbidden() {
        when(documentService.roleOf(DOC, 42L)).thenReturn(Optional.empty());

        assertFalse(handshake("ws://localhost:8080/ws/yjs/" + DOC + "?token=" + validToken));

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void missingTokenIsUnauthorized() {
        assertFalse(handshake("ws://localhost:8080/ws/yjs/" + DOC));

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        assertTrue(attributes.isEmpty());
        verify(documentService, never()).roleOf(anyString(), anyLong());
    }

    @Test
    void invalidTokenIsUnauthorizedAndNeverReachesTheDatabase() {
        assertFalse(handshake("ws://localhost:8080/ws/yjs/" + DOC + "?token=garbage"));

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(documentService, never()).roleOf(anyString(), anyLong());
    }

    @Test
    void expiredTokenIsUnauthorized() {
        String expired = issuer(-60_000L)
                .generateToken(new UserDetailsImpl(42L, "alice", "alice@example.com", null));

        assertFalse(handshake("ws://localhost:8080/ws/yjs/" + DOC + "?token=" + expired));

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void missingDocumentIdInThePathIsUnauthorized() {
        assertFalse(handshake("ws://localhost:8080/ws/yjs/?token=" + validToken));

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenIsUrlDecodedFromTheQueryString() {
        when(documentService.roleOf(DOC, 42L)).thenReturn(Optional.of(Role.EDITOR));
        String encoded = URLEncoder.encode(validToken, StandardCharsets.UTF_8);

        assertTrue(handshake("ws://localhost:8080/ws/yjs/" + DOC + "?token=" + encoded));

        assertEquals(42L, attributes.get(JwtHandshakeInterceptor.ATTR_USER_ID));
    }

    @Test
    void trailingSlashAfterTheDocumentIdIsIgnored() {
        when(documentService.roleOf(DOC, 42L)).thenReturn(Optional.of(Role.EDITOR));

        assertTrue(handshake("ws://localhost:8080/ws/yjs/" + DOC + "/?token=" + validToken));

        assertEquals(DOC, attributes.get(JwtHandshakeInterceptor.ATTR_DOC_ID));
    }

    @Test
    void pathOutsideTheSocketPrefixIsUnauthorized() {
        assertFalse(handshake("ws://localhost:8080/ws/other/" + DOC + "?token=" + validToken));

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(documentService, never()).roleOf(anyString(), anyLong());
    }

    @Test
    void otherQueryParametersDoNotHideTheToken() {
        when(documentService.roleOf(DOC, 42L)).thenReturn(Optional.of(Role.EDITOR));

        assertTrue(handshake("ws://localhost:8080/ws/yjs/" + DOC + "?v=2&token=" + validToken + "&x=1"));

        assertEquals(DOC, attributes.get(JwtHandshakeInterceptor.ATTR_DOC_ID));
    }
}
