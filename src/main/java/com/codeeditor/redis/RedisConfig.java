package com.codeeditor.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Wires the single Redis pub/sub channel used to relay collaboration frames
 * between application nodes.
 */
@Configuration
public class RedisConfig {

    public static final String COLLABORATION_CHANNEL = "collab:relay";

    @Bean
    public ChannelTopic collaborationTopic() {
        return new ChannelTopic(COLLABORATION_CHANNEL);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            CollaborationRelay relay,
            ChannelTopic collaborationTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(relay, collaborationTopic);
        return container;
    }
}
