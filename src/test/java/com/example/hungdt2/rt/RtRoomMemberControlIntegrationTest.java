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
public class RtRoomMemberControlIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    @Test
    public void ownerCanMuteOtherAndUserCanSelfMute() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;
        String suffix = String.valueOf(System.currentTimeMillis());
        RegisterRequest r1 = new RegisterRequest("downer"+suffix,"d"+suffix+"@local","P@ssw0rd123","09990060","Owner");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());
        RegisterRequest r2 = new RegisterRequest("duser"+suffix,"u"+suffix+"@local","P@ssw0rd123","09990061","User");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", new LoginRequest("downer"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", new LoginRequest("duser"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");

        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name","Mute RT Room"); payload.put("isPublic", true);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rt-rooms", new HttpEntity<>(payload, headers1), ApiResponse.class);
        java.util.Map created = (java.util.Map) create.getBody().data();
        Integer roomIdInt = (Integer) created.get("id");
        Long roomId = roomIdInt.longValue();

        // owner and user join
        rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers1), ApiResponse.class);
        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2); headers2.setContentType(MediaType.APPLICATION_JSON);
        rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers2), ApiResponse.class);

        // owner mutes user
        ResponseEntity<ApiResponse> muteResp = rest.postForEntity(base + "/rt-rooms/" + roomId + "/members/" + ((java.util.Map)rr2.getBody().data()).get("id") + "/mute?muted=true", new HttpEntity<>(headers1), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, muteResp.getStatusCode());
        // user should now be muted according to members list
        ResponseEntity<ApiResponse> members = rest.exchange(base + "/rt-rooms/" + roomId + "/members", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        boolean foundMuted = false;
        for (Object o : (java.util.List)members.getBody().data()) {
            java.util.Map mm = (java.util.Map) o;
            if (((Integer)mm.get("userId")).longValue() == ((Integer)((java.util.Map)rr2.getBody().data()).get("id")).longValue()) {
                foundMuted = (Boolean) mm.get("muted"); break;
            }
        }
        Assertions.assertTrue(foundMuted, "User should be muted by owner");

        // user self-mute (toggle to false then true)
        ResponseEntity<ApiResponse> selfMute = rest.postForEntity(base + "/rt-rooms/" + roomId + "/members/" + ((java.util.Map)rr2.getBody().data()).get("id") + "/mute?muted=false", new HttpEntity<>(headers2), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, selfMute.getStatusCode());
        ResponseEntity<ApiResponse> members2 = rest.exchange(base + "/rt-rooms/" + roomId + "/members", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers2), ApiResponse.class);
        boolean nowMuted = false;
        for (Object o : (java.util.List)members2.getBody().data()) {
            java.util.Map mm = (java.util.Map) o;
            if (((Integer)mm.get("userId")).longValue() == ((Integer)((java.util.Map)rr2.getBody().data()).get("id")).longValue()) {
                nowMuted = (Boolean) mm.get("muted"); break;
            }
        }
        Assertions.assertFalse(nowMuted, "User should be able to unmute self");
    }

    @Test
    public void roomMaxMembersEnforced() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;
        String suffix = String.valueOf(System.currentTimeMillis());
        RegisterRequest r1 = new RegisterRequest("cowner"+suffix,"o"+suffix+"@local","P@ssw0rd123","09990070","Owner");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());
        RegisterRequest r2 = new RegisterRequest("cuser1"+suffix,"u1"+suffix+"@local","P@ssw0rd123","09990071","User1");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());
        RegisterRequest r3 = new RegisterRequest("cuser2"+suffix,"u2"+suffix+"@local","P@ssw0rd123","09990072","User2");
        ResponseEntity<ApiResponse> rr3 = rest.postForEntity(base + "/auth/register", r3, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr3.getStatusCode());

        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", new LoginRequest("cowner"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", new LoginRequest("cuser1"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");
        ResponseEntity<ApiResponse> login3 = rest.postForEntity(base + "/auth/login", new LoginRequest("cuser2"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token3 = (String) ((java.util.Map)login3.getBody().data()).get("accessToken");

        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name","Cap RT Room"); payload.put("isPublic", true); payload.put("maxMembers", 2);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rt-rooms", new HttpEntity<>(payload, headers1), ApiResponse.class);
        java.util.Map created = (java.util.Map) create.getBody().data();
        Integer roomIdInt = (Integer) created.get("id");
        Long roomId = roomIdInt.longValue();

        // owner and user1 join => capacity reached
        rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers1), ApiResponse.class);
        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2); headers2.setContentType(MediaType.APPLICATION_JSON);
        rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers2), ApiResponse.class);

        // user2 should fail with ROOM_FULL
        HttpHeaders headers3 = new HttpHeaders(); headers3.add(HttpHeaders.AUTHORIZATION, "Bearer " + token3);
        Assertions.assertThrows(org.springframework.web.client.HttpClientErrorException.Forbidden.class, () -> { rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers3), ApiResponse.class); });
    }

    @Test
    public void joinIsIdempotentAndDoesNotCreateDuplicates() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;
        String suffix = String.valueOf(System.currentTimeMillis());
        RegisterRequest r1 = new RegisterRequest("jowner"+suffix,"o"+suffix+"@local","P@ssw0rd123","09990080","Owner");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());
        RegisterRequest r2 = new RegisterRequest("juser"+suffix,"u"+suffix+"@local","P@ssw0rd123","09990081","User");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", new LoginRequest("jowner"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", new LoginRequest("juser"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");

        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name","Join RT Room"); payload.put("isPublic", true);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rt-rooms", new HttpEntity<>(payload, headers1), ApiResponse.class);
        java.util.Map created = (java.util.Map) create.getBody().data();
        Integer roomIdInt = (Integer) created.get("id");
        Long roomId = roomIdInt.longValue();

        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2); headers2.setContentType(MediaType.APPLICATION_JSON);
        // call join twice
        rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers2), ApiResponse.class);
        rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", new HttpEntity<>(headers2), ApiResponse.class);

        // members list should show the user only once
        ResponseEntity<ApiResponse> membersRes = rest.exchange(base + "/rt-rooms/" + roomId + "/members", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.List members = (java.util.List) membersRes.getBody().data();
        long count = members.stream().filter(it -> ((java.util.Map)it).get("userId").equals(((java.util.Map)rr2.getBody().data()).get("id"))).count();
        Assertions.assertEquals(1, count);
    }
}