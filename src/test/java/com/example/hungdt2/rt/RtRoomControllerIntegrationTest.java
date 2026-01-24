package com.example.hungdt2.rt;

import com.example.hungdt2.auth.dto.LoginRequest;
import com.example.hungdt2.auth.dto.RegisterRequest;
import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.voice.dto.VoiceJoinResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("RT feature disabled")
public class RtRoomControllerIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    @Test
    public void createJoinKickFlow() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;

        RegisterRequest r1 = new RegisterRequest("rtowner","o@local","P@ssw0rd123","09990010","Owner");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());

        RegisterRequest r2 = new RegisterRequest("rtuser","u@local","P@ssw0rd123","09990011","User");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        LoginRequest l1 = new LoginRequest("rtowner","P@ssw0rd123");
        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", l1, ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");

        LoginRequest l2 = new LoginRequest("rtuser","P@ssw0rd123");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", l2, ApiResponse.class);
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");

        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name","RT Test Room"); payload.put("isPublic", true);
        HttpEntity<java.util.Map<String,Object>> createReq = new HttpEntity<>(payload, headers1);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rt-rooms", createReq, ApiResponse.class);
        Assertions.assertTrue(create.getStatusCode() == HttpStatus.OK || create.getStatusCode() == HttpStatus.CREATED);
        java.util.Map created = (java.util.Map) create.getBody().data();
        Integer roomIdInt = (Integer) created.get("id");
        Long roomId = roomIdInt.longValue();

        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2);
        HttpEntity<Void> ent2 = new HttpEntity<>(headers2);
        ResponseEntity<ApiResponse> j2 = rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", ent2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, j2.getStatusCode());
        java.util.Map joinData2 = (java.util.Map) j2.getBody().data();
        Assertions.assertNotNull(joinData2.get("channel"));

        HttpEntity<Void> ent1 = new HttpEntity<>(headers1);
        ResponseEntity<ApiResponse> j1 = rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", ent1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, j1.getStatusCode());

        ResponseEntity<ApiResponse> membersRes = rest.exchange(base + "/rt-rooms/" + roomId + "/members", org.springframework.http.HttpMethod.GET, ent1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, membersRes.getStatusCode());
        java.util.List list = (java.util.List) membersRes.getBody().data();
        Assertions.assertTrue(list.size() >= 2);

        ResponseEntity<ApiResponse> kickRes = rest.postForEntity(base + "/rt-rooms/" + roomId + "/kick?userId=" + ((java.util.Map)rr2.getBody().data()).get("id"), ent1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, kickRes.getStatusCode());

        // u2 cannot rejoin
        Assertions.assertThrows(org.springframework.web.client.HttpClientErrorException.Forbidden.class, () -> { rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", ent2, ApiResponse.class); });
    }

    @Test
    public void invitesAndMembersPermissionsForPrivateRoom() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;

        RegisterRequest r1 = new RegisterRequest("owner2","owner2@local","P@ssw0rd123","09880010","Owner2");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());
        RegisterRequest r2 = new RegisterRequest("stranger","s@local","P@ssw0rd123","09880011","Stranger");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        LoginRequest l1 = new LoginRequest("owner2","P@ssw0rd123");
        String token1 = (String) ((java.util.Map)rest.postForEntity(base + "/auth/login", l1, ApiResponse.class).getBody().data()).get("accessToken");
        LoginRequest l2 = new LoginRequest("stranger","P@ssw0rd123");
        String token2 = (String) ((java.util.Map)rest.postForEntity(base + "/auth/login", l2, ApiResponse.class).getBody().data()).get("accessToken");

        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name","Private Room"); payload.put("isPublic", false);
        HttpEntity<java.util.Map<String,Object>> createReq = new HttpEntity<>(payload, headers1);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rt-rooms", createReq, ApiResponse.class);
        Integer roomIdInt = (Integer) ((java.util.Map) create.getBody().data()).get("id");
        Long roomId = roomIdInt.longValue();

        // stranger cannot list invites (not owner)
        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2);
        HttpEntity<Void> ent2 = new HttpEntity<>(headers2);
        Assertions.assertThrows(org.springframework.web.client.HttpClientErrorException.Forbidden.class, () -> {
            rest.exchange(base + "/rt-rooms/" + roomId + "/invites", org.springframework.http.HttpMethod.GET, ent2, ApiResponse.class);
        });

        // stranger cannot view members of private room
        Assertions.assertThrows(org.springframework.web.client.HttpClientErrorException.Forbidden.class, () -> {
            rest.exchange(base + "/rt-rooms/" + roomId + "/members", org.springframework.http.HttpMethod.GET, ent2, ApiResponse.class);
        });

        // owner can list invites (empty)
        HttpEntity<Void> ent1 = new HttpEntity<>(headers1);
        ResponseEntity<ApiResponse> invitesRes = rest.exchange(base + "/rt-rooms/" + roomId + "/invites", org.springframework.http.HttpMethod.GET, ent1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, invitesRes.getStatusCode());
    }
}

