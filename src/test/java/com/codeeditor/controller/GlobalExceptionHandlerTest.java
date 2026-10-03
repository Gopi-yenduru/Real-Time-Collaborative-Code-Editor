package com.codeeditor.controller;

import com.codeeditor.exception.ConflictException;
import com.codeeditor.exception.ForbiddenException;
import com.codeeditor.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static void assertEnvelope(ResponseEntity<Map<String, Object>> response, HttpStatus expected) {
        assertEquals(expected, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(expected.value(), body.get("status"));
        assertEquals(expected.getReasonPhrase(), body.get("error"));
        assertNotNull(body.get("timestamp"));
        assertNotNull(body.get("message"));
    }

    @Test
    void unmatchedUrlIsNotFoundRatherThanServerError() {
        // The static-resource handler raises this for any URL no controller claims.
        ResponseEntity<Map<String, Object>> response =
                handler.handleNoHandler(new NoResourceFoundException(HttpMethod.GET, "/api/does-not-exist"));

        assertEnvelope(response, HttpStatus.NOT_FOUND);
    }

    @Test
    void wrongHttpMethodIsMethodNotAllowed() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("DELETE"));

        assertEnvelope(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void domainExceptionsMapToTheirStatuses() {
        assertEnvelope(handler.handleNotFound(new NotFoundException("gone")), HttpStatus.NOT_FOUND);
        assertEnvelope(handler.handleConflict(new ConflictException("dupe")), HttpStatus.CONFLICT);
        assertEnvelope(handler.handleForbidden(new ForbiddenException("nope")), HttpStatus.FORBIDDEN);
        assertEnvelope(handler.handleBadRequest(new IllegalArgumentException("bad")), HttpStatus.BAD_REQUEST);
    }

    @Test
    void unexpectedExceptionsStayGenericAndDoNotLeakDetail() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(new RuntimeException("connection string with a password in it"));

        assertEnvelope(response, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
    }
}
