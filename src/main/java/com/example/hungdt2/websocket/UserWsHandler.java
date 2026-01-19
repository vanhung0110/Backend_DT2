package com.example.hungdt2.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserWsHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(UserWsHandler.class);
    private final Map<String, Set<WebSocketSession>> users = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object uidObj = session.getAttributes().get("userId");
        String userId = String.valueOf(uidObj);
        users.computeIfAbsent(userId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(session);
        log.info("User WS open: sessionId={}, userId={}", session.getId(), userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("User WS received: {}", message.getPayload());
        // no-op for now
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        users.values().forEach(set -> set.remove(session));
        log.info("User WS closed session={} status={}", session.getId(), status);
    }

    public void sendToUser(Long userId, Object payload) {
        if (userId == null) return;
        String key = String.valueOf(userId);
        Set<WebSocketSession> list = users.get(key);
        if (list == null || list.isEmpty()) return;
        try {
            String json = mapper.writeValueAsString(payload);
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession s : list) {
                try {
                    if (s.isOpen()) s.sendMessage(msg);
                } catch (Exception e) {
                    log.warn("Failed to send WS msg to user {}: {}", userId, e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to serialize WS payload: {}", ex.getMessage());
        }
    }
}
