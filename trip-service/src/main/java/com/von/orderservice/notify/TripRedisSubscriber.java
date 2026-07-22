package com.von.orderservice.notify;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class TripRedisSubscriber implements MessageListener {

    private final DriverTripWebSocketHandler webSocketHandler;

    public TripRedisSubscriber(DriverTripWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        webSocketHandler.broadcast(new String(message.getBody()));
    }
}
