package com.codeeditor.redis;

import com.codeeditor.config.RedisConfig;
import com.codeeditor.websocket.dto.CursorUpdateMessage;
import com.codeeditor.websocket.dto.OperationAppliedMessage;
import com.codeeditor.websocket.dto.UserJoinedMessage;
import com.codeeditor.websocket.dto.UserLeftMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.debug("Received message from Redis channel {}: {}", channel, body);

        try {
            Map<String, Object> payload;
            
            // Because values may come from Redis as strings, we remove quotes
            if (body.startsWith("\"") && body.endsWith("\"")) {
                 body = body.substring(1, body.length() - 1);
                 body = body.replace("\\\"", "\""); 
            }
            
            payload = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            String docId = (String) payload.get("docId");

            if (RedisConfig.EDITOR_OPS_TOPIC.equals(channel)) {
                OperationAppliedMessage opMsg = objectMapper.convertValue(payload.get("message"), OperationAppliedMessage.class);
                messagingTemplate.convertAndSend("/topic/document/" + docId, opMsg);
            } else if (RedisConfig.EDITOR_CURSOR_TOPIC.equals(channel)) {
                CursorUpdateMessage cursorMsg = objectMapper.convertValue(payload.get("message"), CursorUpdateMessage.class);
                messagingTemplate.convertAndSend("/topic/document/" + docId + "/cursors", cursorMsg);
            } else if (RedisConfig.EDITOR_PRESENCE_TOPIC.equals(channel)) {
                String type = (String) payload.get("type");
                if ("JOIN".equals(type)) {
                    UserJoinedMessage joinMsg = objectMapper.convertValue(payload.get("message"), UserJoinedMessage.class);
                    messagingTemplate.convertAndSend("/topic/document/" + docId + "/presence", joinMsg);
                } else if ("LEAVE".equals(type)) {
                    UserLeftMessage leaveMsg = objectMapper.convertValue(payload.get("message"), UserLeftMessage.class);
                    messagingTemplate.convertAndSend("/topic/document/" + docId + "/presence", leaveMsg);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Redis message", e);
        } catch (Exception e) {
             log.error("Error processing Redis message", e);
        }
    }
}
