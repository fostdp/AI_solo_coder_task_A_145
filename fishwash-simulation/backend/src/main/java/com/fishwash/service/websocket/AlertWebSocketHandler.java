package com.fishwash.service.websocket;

import com.fishwash.entity.AlertRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertWebSocketHandler implements WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyAlert(AlertRecord alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }
}
