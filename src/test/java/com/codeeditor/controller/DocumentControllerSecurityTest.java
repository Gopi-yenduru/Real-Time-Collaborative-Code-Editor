package com.codeeditor.controller;

import com.codeeditor.dto.DocumentDto;
import com.codeeditor.model.Role;
import com.codeeditor.security.AuthEntryPointJwt;
import com.codeeditor.security.JwtAuthenticationFilter;
import com.codeeditor.security.JwtUtil;
import com.codeeditor.security.SecurityConfig;
import com.codeeditor.security.UserDetailsImpl;
import com.codeeditor.security.UserDetailsServiceImpl;
import com.codeeditor.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises the real filter chain in front of the document API: a request is
 * only served when it carries a JWT this server signed, and the authenticated
 * user id reaching the service comes from that token.
 */
@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthEntryPointJwt.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=mvc-test-secret-long-enough-to-derive-a-256-bit-key",
        "jwt.expiration=60000"
})
class DocumentControllerSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;

    @MockitoBean DocumentService documentService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private String tokenFor(long userId, String username) {
        return jwtUtil.generateToken(new UserDetailsImpl(userId, username, username + "@example.com", null));
    }

    @Test
    void requestWithoutATokenIsRejected() throws Exception {
        mvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentService);
    }

    @Test
    void requestWithAValidTokenIsServedWithTheUserIdFromTheClaims() throws Exception {
        when(documentService.listForUser(42L)).thenReturn(List.of(new DocumentDto(
                "doc-1", "Example", "java", null, 42L, "alice", Role.OWNER,
                LocalDateTime.now(), LocalDateTime.now())));

        mvc.perform(get("/api/documents").header("Authorization", "Bearer " + tokenFor(42L, "alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("doc-1"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));

        // The id comes from the token, never from anything client-supplied.
        verify(documentService).listForUser(42L);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        // A well-formed JWT this server did not sign.
        String foreign = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiJhbGljZSIsInVpZCI6NDIsImVtYWlsIjoiYUBiLmMifQ"
                + ".0000000000000000000000000000000000000000000";

        mvc.perform(get("/api/documents").header("Authorization", "Bearer " + foreign))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentService);
    }

    @Test
    void authorizationHeaderWithoutTheBearerPrefixIsIgnored() throws Exception {
        mvc.perform(get("/api/documents").header("Authorization", tokenFor(42L, "alice")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentService);
    }

    @Test
    void garbageTokenIsRejectedRatherThanErroring() throws Exception {
        mvc.perform(get("/api/documents").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        verify(documentService, never()).listForUser(anyLong());
    }
}
