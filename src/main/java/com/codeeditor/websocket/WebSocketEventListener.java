package com.codeeditor.websocket;

import com.codeeditor.redis.RedisMessagePublisher;
import com.codeeditor.security.UserDetailsImpl;
import com.codeeditor.service.SessionManager;
import com.codeeditor.websocket.dto.UserLeftMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SessionManager sessionManager;
    private final RedisMessagePublisher redisPublisher;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket connection closed for session: {}", sessionId);

        String[] userAndDoc = sessionManager.getUserAndDocBySessionId(sessionId);
        if (userAndDoc != null && userAndDoc.length == 2) {
            Long userId = Long.parseLong(userAndDoc[0]);
            String docId = userAndDoc[1];

            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
                 if (userDetails.getId().equals(userId)) {
                      sessionManager.removeUserFromSession(sessionId);
                      
                      UserLeftMessage leaveMsg = UserLeftMessage.builder()
                              .userId(userId)
                              .build();
                              
                      redisPublisher.publishUserLeft(docId, leaveMsg);
                 }
            }
        }
    }
}
