package com.fishwash.service.websocket;

import com.fishwash.entity.AlertRecord;

public interface WebSocketNotifier {
    void notifyAlert(AlertRecord alert);
}
