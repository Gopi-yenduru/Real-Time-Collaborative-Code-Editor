package com.codeeditor.service;

import com.codeeditor.dto.AuthResponse;
import com.codeeditor.dto.UserDto;
import com.codeeditor.model.User;
import com.codeeditor.repository.UserRepository;
import com.codeeditor.security.JwtUtil;
import com.codeeditor.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails);

        return new AuthResponse(
                jwt,
                jwtUtil.getJwtExpirationMs(),
                new UserDto(userDetails.getId(), userDetails.getUsername(), userDetails.getEmail()));
    }

    public UserDto register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(encoder.encode(password))
                .build();

        return UserDto.from(userRepository.save(user));
    }

    public UserDto getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserDto::from)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
