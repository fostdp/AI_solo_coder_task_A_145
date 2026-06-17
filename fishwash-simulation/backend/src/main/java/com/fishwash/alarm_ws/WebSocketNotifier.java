package com.fishwash.alarm_ws;

import com.fishwash.entity.AlertRecord;

public interface WebSocketNotifier {
    void notifyAlert(AlertRecord alert);
}
