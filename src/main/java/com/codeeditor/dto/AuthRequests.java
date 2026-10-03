package com.codeeditor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Auth-related request bodies. */
public final class AuthRequests {

    private AuthRequests() {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(min = 6, max = 100) String password
    ) {
    }
}
