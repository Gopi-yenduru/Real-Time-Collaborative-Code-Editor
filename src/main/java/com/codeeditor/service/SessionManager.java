package com.codeeditor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManager {

    private final StringRedisTemplate redisTemplate;
    
    // Redis key formats
    // session:doc:{docId} -> Set of userIds
    // session:color:{docId}:{userId} -> hex color string
    // session:user:{sessionId} -> {userId}:{docId} (for mapping websocket session to user/doc on disconnect)

    private static final String DOC_SESSIONS_PREFIX = "session:doc:";
    private static final String USER_COLOR_PREFIX = "session:color:";
    private static final String WS_SESSION_PREFIX = "session:ws:";

    public void addUserToSession(String docId, Long userId, String wsSessionId) {
        String docKey = DOC_SESSIONS_PREFIX + docId;
        redisTemplate.opsForSet().add(docKey, String.valueOf(userId));

        // Assign random color if not exists
        String colorKey = USER_COLOR_PREFIX + docId + ":" + userId;
        redisTemplate.opsForValue().setIfAbsent(colorKey, generateRandomHexColor());

        // Map WS session to this user/doc combo
        if (wsSessionId != null) {
            redisTemplate.opsForValue().set(WS_SESSION_PREFIX + wsSessionId, userId + ":" + docId);
        }
        
        log.info("User {} joined document {}", userId, docId);
    }

    public void removeUserFromSession(String wsSessionId) {
        String mapping = redisTemplate.opsForValue().get(WS_SESSION_PREFIX + wsSessionId);
        if (mapping != null) {
            String[] parts = mapping.split(":");
            if (parts.length == 2) {
                String userId = parts[0];
                String docId = parts[1];
                
                removeUserFromDocument(docId, Long.parseLong(userId));
                redisTemplate.delete(WS_SESSION_PREFIX + wsSessionId);
            }
        }
    }
    
    public void removeUserFromDocument(String docId, Long userId) {
        String docKey = DOC_SESSIONS_PREFIX + docId;
        redisTemplate.opsForSet().remove(docKey, String.valueOf(userId));
        
        String colorKey = USER_COLOR_PREFIX + docId + ":" + userId;
        redisTemplate.delete(colorKey);
        
        log.info("User {} left document {}", userId, docId);
    }

    public Set<Long> getActiveUsers(String docId) {
        String docKey = DOC_SESSIONS_PREFIX + docId;
        Set<String> userIds = redisTemplate.opsForSet().members(docKey);
        if (userIds == null) {
            return Set.of();
        }
        return userIds.stream().map(Long::parseLong).collect(Collectors.toSet());
    }

    public String getUserColor(String docId, Long userId) {
        String colorKey = USER_COLOR_PREFIX + docId + ":" + userId;
        String color = redisTemplate.opsForValue().get(colorKey);
        if (color == null) {
            color = generateRandomHexColor();
            redisTemplate.opsForValue().set(colorKey, color);
        }
        return color;
    }
    
    public String[] getUserAndDocBySessionId(String wsSessionId) {
         String mapping = redisTemplate.opsForValue().get(WS_SESSION_PREFIX + wsSessionId);
         if (mapping != null) {
             return mapping.split(":");
         }
         return null;
    }

    private String generateRandomHexColor() {
        // Generate vivid colors
        int r = (int) (Math.random() * 127) + 128; // 128-255
        int g = (int) (Math.random() * 127) + 128;
        int b = (int) (Math.random() * 127) + 128;
        return String.format("#%02x%02x%02x", r, g, b);
    }
}
