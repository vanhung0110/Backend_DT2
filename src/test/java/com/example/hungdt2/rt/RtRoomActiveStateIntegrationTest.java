package com.example.hungdt2.rt;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RtRoomActiveStateIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.hungdt2.rt.RtRoomService rtRoomService;

    @Test
    public void activeStateDependsOnOnlineCount() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;

        String suffix = String.valueOf(System.currentTimeMillis());
        RegisterRequest r1 = new RegisterRequest("aowner"+suffix,"a"+suffix+"@local","P@ssw0rd123","09990030","Owner");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());

        RegisterRequest r2 = new RegisterRequest("auser"+suffix,"u"+suffix+"@local","P@ssw0rd123","09990031","User");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", new LoginRequest("aowner"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", new LoginRequest("auser"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");

        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name","State RT Room"); payload.put("isPublic", true);
        HttpEntity<java.util.Map<String,Object>> createReq = new HttpEntity<>(payload, headers1);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rt-rooms", createReq, ApiResponse.class);
        Assertions.assertTrue(create.getStatusCode() == HttpStatus.OK || create.getStatusCode() == HttpStatus.CREATED);
        java.util.Map created = (java.util.Map) create.getBody().data();
        Integer roomIdInt = (Integer) created.get("id");
        Long roomId = roomIdInt.longValue();

        // owner joins
        ResponseEntity<ApiResponse> j1 = rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers1), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, j1.getStatusCode());

        // room should be inactive with only 1 online
        ResponseEntity<ApiResponse> rGet1 = rest.exchange(base + "/rt-rooms/" + roomId, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.Map roomObj1 = (java.util.Map) rGet1.getBody().data();
        Assertions.assertFalse((Boolean) roomObj1.get("active"), "Room should be inactive with only 1 online");

        // second user joins
        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2); headers2.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<ApiResponse> j2 = rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers2), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, j2.getStatusCode());

        // room should be active now
        ResponseEntity<ApiResponse> rGet2 = rest.exchange(base + "/rt-rooms/" + roomId, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.Map roomObj2 = (java.util.Map) rGet2.getBody().data();
        Assertions.assertTrue((Boolean) roomObj2.get("active"), "Room should be active with 2 online");

        // simulate websocket disconnect for second user (no REST leave)
        // call service.onWsDisconnect directly
        Integer user2IdInt = (Integer) ((java.util.Map) rr2.getBody().data()).get("id");
        Long user2Id = user2IdInt.longValue();
        rtRoomService.onWsDisconnect(roomId, user2Id);

        // room should be inactive again
        ResponseEntity<ApiResponse> rGet3 = rest.exchange(base + "/rt-rooms/" + roomId, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.Map roomObj3 = (java.util.Map) rGet3.getBody().data();
        Assertions.assertFalse((Boolean) roomObj3.get("active"), "Room should be inactive after simulated disconnect");
    }

    @Test
    public void websocketDisconnectUpdatesPresence() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;
        String suffix = String.valueOf(System.currentTimeMillis());
        RegisterRequest r1 = new RegisterRequest("bowner"+suffix,"b"+suffix+"@local","P@ssw0rd123","09990040","Owner");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());
        RegisterRequest r2 = new RegisterRequest("buser"+suffix,"u2"+suffix+"@local","P@ssw0rd123","09990041","User");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", new LoginRequest("bowner"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", new LoginRequest("buser"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");

        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name","Presence RT Room"); payload.put("isPublic", true);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rt-rooms", new HttpEntity<>(payload, headers1), ApiResponse.class);
        java.util.Map created = (java.util.Map) create.getBody().data();
        Integer roomIdInt = (Integer) created.get("id");
        Long roomId = roomIdInt.longValue();

        // both join
        rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers1), ApiResponse.class);
        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2); headers2.setContentType(MediaType.APPLICATION_JSON);
        rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers2), ApiResponse.class);

        // room should be active
        ResponseEntity<ApiResponse> rGet2 = rest.exchange(base + "/rt-rooms/" + roomId, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.Map roomObj2 = (java.util.Map) rGet2.getBody().data();
        Assertions.assertTrue((Boolean) roomObj2.get("active"), "Room should be active with 2 online");

        // simulate ws disconnect for second user
        Integer user2IdInt = (Integer) ((java.util.Map) rr2.getBody().data()).get("id");
        Long user2Id = user2IdInt.longValue();
        rtRoomService.onWsDisconnect(roomId, user2Id);

        // room should become inactive
        ResponseEntity<ApiResponse> rGet3 = rest.exchange(base + "/rt-rooms/" + roomId, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.Map roomObj3 = (java.util.Map) rGet3.getBody().data();
        Assertions.assertFalse((Boolean) roomObj3.get("active"), "Room should be inactive after simulated ws disconnect");
    }
}

