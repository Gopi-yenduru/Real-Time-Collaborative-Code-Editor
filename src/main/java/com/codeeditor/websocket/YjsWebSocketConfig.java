package com.codeeditor.websocket;

import com.codeeditor.security.JwtUtil;
import com.codeeditor.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.Arrays;

/**
 * Registers the native (binary) collaboration WebSocket at
 * {@code /ws/yjs/{documentId}}, guarded by {@link JwtHandshakeInterceptor}.
 * Plain WebSocket — not STOMP/SockJS — because Yjs exchanges compact binary
 * frames that don't need a text-based sub-protocol.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class YjsWebSocketConfig implements WebSocketConfigurer {

    private static final int MAX_MESSAGE_BYTES = 4 * 1024 * 1024; // 4 MB

    private final YjsWebSocketHandler handler;
    private final JwtUtil jwtUtil;
    private final DocumentService documentService;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        registry.addHandler(handler, "/ws/yjs/*")
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil, documentService))
                .setAllowedOriginPatterns(origins);
    }

    /** Raise the container's inbound buffer limits to match {@code MAX_MESSAGE_BYTES}. */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(MAX_MESSAGE_BYTES);
        container.setMaxTextMessageBufferSize(MAX_MESSAGE_BYTES);
        return container;
    }
}
