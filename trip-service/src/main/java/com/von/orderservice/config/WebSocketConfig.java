package com.von.orderservice.config;

import com.von.orderservice.notify.DriverTripWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.von.orderservice.notify.TripNotificationPublisher;
import com.von.orderservice.notify.TripRedisSubscriber;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DriverTripWebSocketHandler driverTripWebSocketHandler;

    public WebSocketConfig(DriverTripWebSocketHandler driverTripWebSocketHandler) {
        this.driverTripWebSocketHandler = driverTripWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(driverTripWebSocketHandler, "/ws/driver/trips")
                .setAllowedOrigins("*");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            TripRedisSubscriber tripRedisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(tripRedisSubscriber, new ChannelTopic(TripNotificationPublisher.CHANNEL));
        return container;
    }
}
