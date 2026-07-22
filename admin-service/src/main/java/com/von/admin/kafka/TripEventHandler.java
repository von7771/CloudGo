package com.von.admin.kafka;

import com.von.common.event.TripEventMessage;

public interface TripEventHandler {

    void handle(TripEventMessage message);
}
