package com.example.hungdt2.websocket;

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
public class RoomWsHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(RoomWsHandler.class);

    // roomId -> sessions (legacy / for broadcasts)
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    // roomId -> (userId -> sessions) to track per-user multi-session presence
    private final Map<String, Map<String, Set<WebSocketSession>>> roomUserSessions = new ConcurrentHashMap<>();

    public RoomWsHandler() {
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        if (roomId == null) {
            String path = session.getUri().getPath(); // /ws/rooms/{id}
            String[] p = path.split("/");
            roomId = p[p.length - 1];
        }
        // add to broadcast list
        rooms.computeIfAbsent(roomId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(session);
        // track per-user sessions
        Object uidObj = session.getAttributes().get("userId");
        String userIdStr = String.valueOf(uidObj);
        if (userIdStr != null && !"null".equals(userIdStr)) {
            roomUserSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(userIdStr, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                    .add(session);
        }
        log.info("WS open: sessionId={}, roomId={}, userId={}, attrs={}", session.getId(), roomId, userIdStr,
                session.getAttributes());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        if (roomId == null) {
            String path = session.getUri().getPath();
            String[] p = path.split("/");
            roomId = p[p.length - 1];
        }
        log.info("WS received (room={}): {}", roomId, message.getPayload());
        Set<WebSocketSession> list = rooms.get(roomId);
        if (list != null) {
            for (WebSocketSession s : list) {
                if (s.isOpen())
                    s.sendMessage(message);
            }
        }
    }

    // Programmatic broadcast to a room
    public void sendToRoom(String roomId, java.util.Map<String, Object> payload) {
        try {
            Set<WebSocketSession> list = rooms.get(roomId);
            if (list == null)
                return;
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession s : list) {
                try {
                    if (s.isOpen())
                        s.sendMessage(msg);
                } catch (Exception e) {
                    log.warn("Failed to send WS msg to session {}: {}", s.getId(), e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to broadcast to room {}: {}", roomId, ex.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Remove from broadcast list
        rooms.values().forEach(set -> set.remove(session));
        // Remove from per-user tracking and only mark offline when no more sessions for
        // that user in the room
        try {
            Object uidObj = session.getAttributes().get("userId");
            String userIdStr = String.valueOf(uidObj);
            Long userId = null;
            try {
                userId = Long.valueOf(userIdStr);
            } catch (Exception ex) {
                userId = null;
            }
            String roomId = (String) session.getAttributes().get("roomId");
            if (roomId == null) {
                String path = session.getUri().getPath(); // /ws/rooms/{id}
                String[] p = path.split("/");
                roomId = p[p.length - 1];
            }
            if (roomId != null && userId != null) {
                // remove this session from user's set
                Map<String, Set<WebSocketSession>> usersMap = roomUserSessions.get(roomId);
                if (usersMap != null) {
                    Set<WebSocketSession> userSet = usersMap.get(userIdStr);
                    if (userSet != null) {
                        userSet.remove(session);
                        if (userSet.isEmpty()) {
                            // no more sessions for this user in the room -> mark offline
                            usersMap.remove(userIdStr);
                            // rtRoomService usage removed
                        }
                    }
                    if (usersMap.isEmpty()) {
                        roomUserSessions.remove(roomId);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("No room/user info on WS session: {}", e.getMessage());
        }
        log.info("WS closed session={} status={}", session.getId(), status);
    }
}
