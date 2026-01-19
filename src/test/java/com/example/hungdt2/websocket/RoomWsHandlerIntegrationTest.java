package com.example.hungdt2.websocket;

import com.example.hungdt2.auth.dto.LoginRequest;
import com.example.hungdt2.auth.dto.RegisterRequest;
import com.example.hungdt2.common.ApiResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketExtension;

import java.net.URI;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RoomWsHandlerIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.hungdt2.websocket.RoomWsHandler roomWsHandler;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.hungdt2.rt.RtRoomService rtRoomService;

    @Test
    public void multipleSessionsKeepUserOnlineUntilAllClosed() throws Exception {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;
        String suffix = String.valueOf(System.currentTimeMillis());
        RegisterRequest r1 = new RegisterRequest("cowner"+suffix,"c"+suffix+"@local","P@ssw0rd123","09990050","Owner");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());
        RegisterRequest r2 = new RegisterRequest("cuser"+suffix,"u"+suffix+"@local","P@ssw0rd123","09990051","User");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", new LoginRequest("cowner"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", new LoginRequest("cuser"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");

        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name","WS Session RT Room"); payload.put("isPublic", true);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rt-rooms", new HttpEntity<>(payload, headers1), ApiResponse.class);
        java.util.Map created = (java.util.Map) create.getBody().data();
        Integer roomIdInt = (Integer) created.get("id");
        Long roomId = roomIdInt.longValue();

        // both join (owner and user)
        rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers1), ApiResponse.class);
        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2); headers2.setContentType(MediaType.APPLICATION_JSON);
        rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers2), ApiResponse.class);

        // Ensure active
        ResponseEntity<ApiResponse> rGet2 = rest.exchange(base + "/rt-rooms/" + roomId, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.Map roomObj2 = (java.util.Map) rGet2.getBody().data();
        Assertions.assertTrue((Boolean) roomObj2.get("active"));

        // simulate two websocket sessions for user2
        Integer user2IdInt = (Integer) ((java.util.Map) rr2.getBody().data()).get("id");
        Long user2Id = user2IdInt.longValue();
        MockWsSession s1 = new MockWsSession("s1", roomId, user2Id);
        MockWsSession s2 = new MockWsSession("s2", roomId, user2Id);

        // establish both
        roomWsHandler.afterConnectionEstablished(s1);
        roomWsHandler.afterConnectionEstablished(s2);

        // close only first -> user should still be online
        roomWsHandler.afterConnectionClosed(s1, CloseStatus.NORMAL);
        ResponseEntity<ApiResponse> m1 = rest.exchange(base + "/rt-rooms/" + roomId + "/members", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.List members1 = (java.util.List) m1.getBody().data();
        boolean stillOnline = false;
        for (Object o : members1) {
            java.util.Map mm = (java.util.Map) o;
            Integer uid = (Integer) mm.get("userId");
            if (uid.longValue() == user2Id) { stillOnline = (Boolean) mm.get("online"); break; }
        }
        Assertions.assertTrue(stillOnline, "User should remain online after one session closed");

        // close second -> user should now be offline
        roomWsHandler.afterConnectionClosed(s2, CloseStatus.NORMAL);
        ResponseEntity<ApiResponse> m2 = rest.exchange(base + "/rt-rooms/" + roomId + "/members", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.List members2 = (java.util.List) m2.getBody().data();
        boolean nowOffline = true;
        for (Object o : members2) {
            java.util.Map mm = (java.util.Map) o;
            Integer uid = (Integer) mm.get("userId");
            if (uid.longValue() == user2Id) { nowOffline = !((Boolean) mm.get("online")); break; }
        }
        Assertions.assertTrue(nowOffline, "User should be offline after all sessions closed");
    }

    // Minimal mock WebSocketSession for tests
    static class MockWsSession implements WebSocketSession {
        private final String id;
        private final Map<String, Object> attrs = new ConcurrentHashMap<>();
        private final URI uri;

        MockWsSession(String id, Long roomId, Long userId) throws Exception {
            this.id = id;
            attrs.put("roomId", String.valueOf(roomId));
            attrs.put("userId", String.valueOf(userId));
            this.uri = new URI("/ws/rooms/" + roomId);
        }

        @Override public String getId() { return id; }
        @Override public URI getUri() { return uri; }
        @Override public Map<String, Object> getAttributes() { return attrs; }
        // the rest can be no-op / defaults
        @Override public org.springframework.http.HttpHeaders getHandshakeHeaders() { return null; }
        @Override public Principal getPrincipal() { return null; }
        @Override public InetSocketAddress getLocalAddress() { return null; }
        @Override public InetSocketAddress getRemoteAddress() { return null; }
        @Override public String getAcceptedProtocol() { return null; }
        @Override public void setTextMessageSizeLimit(int messageSizeLimit) {}
        @Override public int getTextMessageSizeLimit() { return 0; }
        @Override public void setBinaryMessageSizeLimit(int messageSizeLimit) {}
        @Override public int getBinaryMessageSizeLimit() { return 0; }
        @Override public List<WebSocketExtension> getExtensions() { return null; }
        @Override public void sendMessage(org.springframework.web.socket.WebSocketMessage<?> message) throws java.io.IOException {}
        @Override public boolean isOpen() { return true; }
        @Override public void close() throws java.io.IOException {}
        @Override public void close(CloseStatus status) throws java.io.IOException {}
    }
}
