package com.codeeditor.redis;

import com.codeeditor.websocket.Frames;
import com.codeeditor.websocket.YjsSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Bridges collaboration frames across application instances over Redis pub/sub,
 * so users editing the same document stay in sync even when connected to
 * different nodes behind a load balancer.
 *
 * <p>The node that receives a client frame is the one that persists it (for SYNC).
 * Peers only re-broadcast to their own local sessions — never re-persist — which
 * keeps the update log free of duplicates from fan-out.
 */
@Component
@Slf4j
public class CollaborationRelay implements MessageListener {

    /** Unique per JVM; lets a node ignore the messages it published itself. */
    private final String nodeId = UUID.randomUUID().toString();

    private final StringRedisTemplate redis;
    private final ChannelTopic topic;
    private final ObjectMapper mapper;
    private final YjsSessionRegistry registry;

    public CollaborationRelay(StringRedisTemplate redis, ChannelTopic collaborationTopic,
                              YjsSessionRegistry registry) {
        this.redis = redis;
        this.topic = collaborationTopic;
        this.registry = registry;
        this.mapper = new ObjectMapper();
    }

    /** Publish a frame to peer nodes. */
    public void publish(String documentId, byte type, byte[] payload) {
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "docId", documentId,
                    "node", nodeId,
                    "type", (int) type,
                    "data", Base64.getEncoder().encodeToString(payload == null ? new byte[0] : payload)));
            redis.convertAndSend(topic.getTopic(), json);
        } catch (Exception e) {
            log.warn("Failed to publish collaboration frame: {}", e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> payload = mapper.readValue(json, Map.class);

            String origin = (String) payload.get("node");
            if (nodeId.equals(origin)) {
                return; // our own message, already delivered locally
            }

            String documentId = (String) payload.get("docId");
            byte type = ((Number) payload.get("type")).byteValue();
            byte[] data = Base64.getDecoder().decode((String) payload.get("data"));

            registry.broadcast(documentId, Frames.encode(type, data), null);
        } catch (Exception e) {
            log.warn("Failed to handle relayed collaboration frame: {}", e.getMessage());
        }
    }
}
