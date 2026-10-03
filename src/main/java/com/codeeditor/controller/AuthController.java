package com.codeeditor.controller;

import com.codeeditor.dto.AuthRequests.LoginRequest;
import com.codeeditor.dto.AuthRequests.RegisterRequest;
import com.codeeditor.dto.AuthResponse;
import com.codeeditor.dto.UserDto;
import com.codeeditor.security.UserDetailsImpl;
import com.codeeditor.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, log in, and inspect the current session")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/register")
    @Operation(summary = "Create a new account")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        UserDto user = authService.register(request.username(), request.email(), request.password());
        return ResponseEntity.status(201).body(user);
    }

    @GetMapping("/me")
    @Operation(summary = "Return the currently authenticated user")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
    }
}
