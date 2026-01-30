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
        try {
            com.fasterxml.jackson.databind.JsonNode payload = mapper.readTree(message.getPayload());
            if (!payload.has("type") || !payload.has("targetUserId")) {
                log.warn("Invalid signal message from user, missing type or targetUserId: {}", message.getPayload());
                return;
            }

            String type = payload.get("type").asText();
            Long targetUserId = payload.get("targetUserId").asLong();

            // Get sender ID
            Object uidObj = session.getAttributes().get("userId");
            Long senderId = Long.valueOf(String.valueOf(uidObj));

            log.info("Signal received: type={} from={} to={}", type, senderId, targetUserId);

            // Construct forward message
            com.fasterxml.jackson.databind.node.ObjectNode forwardMsg = (com.fasterxml.jackson.databind.node.ObjectNode) payload;
            forwardMsg.put("senderId", senderId);

            // Forward to target user
            boolean sent = sendToUser(targetUserId, forwardMsg);

            // If target unreachable and it's a call initiation, inform sender immediately
            if (!sent && "call-user".equals(type)) {
                com.fasterxml.jackson.databind.node.ObjectNode rejectMsg = mapper.createObjectNode();
                rejectMsg.put("type", "reject-call");
                rejectMsg.put("senderId", targetUserId); // Pretend it's from target
                rejectMsg.put("targetUserId", senderId);
                rejectMsg.put("reason", "offline");
                sendToUser(senderId, rejectMsg);
            }

        } catch (Exception e) {
            log.error("Error handling signal message: {}", e.getMessage());
        }
    }

    public boolean sendToUser(Long userId, Object payload) {
        if (userId == null)
            return false;
        String key = String.valueOf(userId);
        Set<WebSocketSession> list = users.get(key);
        if (list == null || list.isEmpty()) {
            return false;
        }
        boolean anySent = false;
        try {
            String json = mapper.writeValueAsString(payload);
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession s : list) {
                try {
                    synchronized (s) { // Prevent concurrent sends on same session
                        if (s.isOpen()) {
                            s.sendMessage(msg);
                            anySent = true;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to send WS msg to user {}: {}", userId, e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to serialize WS payload: {}", ex.getMessage());
            return false;
        }
        return anySent;
    }
}
