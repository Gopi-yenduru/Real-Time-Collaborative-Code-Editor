package com.codeeditor.redis;

import com.codeeditor.config.RedisConfig;
import com.codeeditor.websocket.dto.CursorUpdateMessage;
import com.codeeditor.websocket.dto.OperationAppliedMessage;
import com.codeeditor.websocket.dto.UserJoinedMessage;
import com.codeeditor.websocket.dto.UserLeftMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisMessagePublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void publishOperation(String docId, OperationAppliedMessage message) {
        log.debug("Publishing operation for docId: {}", docId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("docId", docId);
        payload.put("message", message);
        redisTemplate.convertAndSend(RedisConfig.EDITOR_OPS_TOPIC, serialize(payload));
    }

    public void publishCursorUpdate(String docId, CursorUpdateMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("docId", docId);
        payload.put("message", message);
        redisTemplate.convertAndSend(RedisConfig.EDITOR_CURSOR_TOPIC, serialize(payload));
    }

    public void publishUserJoined(String docId, UserJoinedMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("docId", docId);
        payload.put("type", "JOIN");
        payload.put("message", message);
        redisTemplate.convertAndSend(RedisConfig.EDITOR_PRESENCE_TOPIC, serialize(payload));
    }

    public void publishUserLeft(String docId, UserLeftMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("docId", docId);
        payload.put("type", "LEAVE");
        payload.put("message", message);
        redisTemplate.convertAndSend(RedisConfig.EDITOR_PRESENCE_TOPIC, serialize(payload));
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Redis message", e);
            return "{}";
        }
    }
}
