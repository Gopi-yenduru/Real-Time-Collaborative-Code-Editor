package com.codeeditor.dto;

/** Returned on successful login: a bearer token plus the authenticated user. */
public record AuthResponse(String token, long expiresInMs, UserDto user) {
}
