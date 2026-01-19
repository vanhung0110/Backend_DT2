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
public class RtRoomInviteIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    @Test
    public void privateRoomInviteFlow() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;

        String suffix = String.valueOf(System.currentTimeMillis());
        RegisterRequest r1 = new RegisterRequest("powner"+suffix,"o"+suffix+"@local","P@ssw0rd123","09990020","Owner");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());

        RegisterRequest r2 = new RegisterRequest("puser"+suffix,"u"+suffix+"@local","P@ssw0rd123","09990021","User");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        RegisterRequest r3 = new RegisterRequest("pother"+suffix,"o2"+suffix+"@local","P@ssw0rd123","09990022","Other");
        ResponseEntity<ApiResponse> rr3 = rest.postForEntity(base + "/auth/register", r3, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr3.getStatusCode());

        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", new LoginRequest("powner"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", new LoginRequest("puser"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");
        ResponseEntity<ApiResponse> login3 = rest.postForEntity(base + "/auth/login", new LoginRequest("pother"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token3 = (String) ((java.util.Map)login3.getBody().data()).get("accessToken");

        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name","Private RT Room"); payload.put("isPublic", false);
        HttpEntity<java.util.Map<String,Object>> createReq = new HttpEntity<>(payload, headers1);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rt-rooms", createReq, ApiResponse.class);
        Assertions.assertTrue(create.getStatusCode() == HttpStatus.OK || create.getStatusCode() == HttpStatus.CREATED);
        java.util.Map created = (java.util.Map) create.getBody().data();
        Integer roomIdInt = (Integer) created.get("id");
        Long roomId = roomIdInt.longValue();

        // pother (not invited) should be forbidden to join
        HttpHeaders headers3 = new HttpHeaders(); headers3.add(HttpHeaders.AUTHORIZATION, "Bearer " + token3);
        HttpEntity<Void> ent3 = new HttpEntity<>(headers3);
        Assertions.assertThrows(org.springframework.web.client.HttpClientErrorException.Forbidden.class, () -> { rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", ent3, ApiResponse.class); });

        // owner invites puser
        ResponseEntity<ApiResponse> inviteRes = rest.postForEntity(base + "/rt-rooms/" + roomId + "/invite?userId=" + ((java.util.Map)rr2.getBody().data()).get("id"), new HttpEntity<>(headers1), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, inviteRes.getStatusCode());

        // puser still cannot join until they accept the invite
        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2);
        HttpEntity<Void> ent2 = new HttpEntity<>(headers2);
        Assertions.assertThrows(org.springframework.web.client.HttpClientErrorException.Forbidden.class, () -> { rest.postForEntity(base + "/rt-rooms/" + roomId + "/join", ent2, ApiResponse.class); });

        // owner lists invites to get invite id
        ResponseEntity<ApiResponse> invitesForRoom = rest.exchange(base + "/rt-rooms/" + roomId + "/invites", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.List invitesList = (java.util.List) invitesForRoom.getBody().data();
        Assertions.assertFalse(invitesList.isEmpty());
        Integer inviteIdInt = (Integer) ((java.util.Map)invitesList.get(0)).get("id");
        Long inviteId = inviteIdInt.longValue();

        // puser accepts invite
        ResponseEntity<ApiResponse> acceptRes = rest.postForEntity(base + "/rt-rooms/" + roomId + "/invites/" + inviteId + "/accept", ent2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, acceptRes.getStatusCode());

        // ensure puser is member now
        ResponseEntity<ApiResponse> membersRes = rest.exchange(base + "/rt-rooms/" + roomId + "/members", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.List members = (java.util.List) membersRes.getBody().data();
        boolean found = members.stream().anyMatch(it -> ((java.util.Map)it).get("userId").equals(((java.util.Map)rr2.getBody().data()).get("id")));
        Assertions.assertTrue(found);
    }

    @Test
    public void inviteIdempotencyAndReinviteFlow() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;

        String longSuffix = String.valueOf(System.currentTimeMillis());
        String suffix = longSuffix + "-2";
        String shortSuffix = longSuffix.substring(Math.max(0, longSuffix.length()-6));
        RegisterRequest r1 = new RegisterRequest("rioowner"+suffix,"o"+suffix+"@local","P@ssw0rd123","099900" + shortSuffix,"Owner");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());

        RegisterRequest r2 = new RegisterRequest("riouser"+suffix,"u"+suffix+"@local","P@ssw0rd123","099901" + shortSuffix,"User");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", new LoginRequest("rioowner"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", new LoginRequest("riouser"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");

        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name","Private RT Room"); payload.put("isPublic", false);
        HttpEntity<java.util.Map<String,Object>> createReq = new HttpEntity<>(payload, headers1);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rt-rooms", createReq, ApiResponse.class);
        Assertions.assertTrue(create.getStatusCode() == HttpStatus.OK || create.getStatusCode() == HttpStatus.CREATED);
        java.util.Map created = (java.util.Map) create.getBody().data();
        Integer roomIdInt = (Integer) created.get("id");
        Long roomId = roomIdInt.longValue();

        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2);
        HttpEntity<Void> ent2 = new HttpEntity<>(headers2);

        // owner invites user
        ResponseEntity<ApiResponse> inviteRes = rest.postForEntity(base + "/rt-rooms/" + roomId + "/invite?userId=" + ((java.util.Map)rr2.getBody().data()).get("id"), new HttpEntity<>(headers1), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, inviteRes.getStatusCode());

        // owner lists invites to get invite id
        ResponseEntity<ApiResponse> invitesForRoom = rest.exchange(base + "/rt-rooms/" + roomId + "/invites", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.List invitesList = (java.util.List) invitesForRoom.getBody().data();
        Assertions.assertFalse(invitesList.isEmpty());
        Integer inviteIdInt = (Integer) ((java.util.Map)invitesList.get(0)).get("id");
        Long inviteId = inviteIdInt.longValue();

        // user rejects invite
        ResponseEntity<ApiResponse> rejectRes = rest.postForEntity(base + "/rt-rooms/" + roomId + "/invites/" + inviteId + "/reject", ent2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, rejectRes.getStatusCode());

        // reject again (idempotent)
        ResponseEntity<ApiResponse> rejectRes2 = rest.postForEntity(base + "/rt-rooms/" + roomId + "/invites/" + inviteId + "/reject", ent2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, rejectRes2.getStatusCode());

        // owner re-invites
        ResponseEntity<ApiResponse> reinvite = rest.postForEntity(base + "/rt-rooms/" + roomId + "/invite?userId=" + ((java.util.Map)rr2.getBody().data()).get("id"), new HttpEntity<>(headers1), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, reinvite.getStatusCode());

        // invite should be PENDING again
        ResponseEntity<ApiResponse> invitesForRoom2 = rest.exchange(base + "/rt-rooms/" + roomId + "/invites", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.List invitesList2 = (java.util.List) invitesForRoom2.getBody().data();
        Assertions.assertFalse(invitesList2.isEmpty());
        String status = (String) ((java.util.Map)invitesList2.get(0)).get("status");
        Assertions.assertEquals("PENDING", status);

        // user accepts
        ResponseEntity<ApiResponse> acceptRes = rest.postForEntity(base + "/rt-rooms/" + roomId + "/invites/" + inviteId + "/accept", ent2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, acceptRes.getStatusCode());

        // accept again (idempotent)
        ResponseEntity<ApiResponse> acceptRes2 = rest.postForEntity(base + "/rt-rooms/" + roomId + "/invites/" + inviteId + "/accept", ent2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, acceptRes2.getStatusCode());

        // ensure user is member now
        ResponseEntity<ApiResponse> membersRes = rest.exchange(base + "/rt-rooms/" + roomId + "/members", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers1), ApiResponse.class);
        java.util.List members = (java.util.List) membersRes.getBody().data();
        boolean found = members.stream().anyMatch(it -> ((java.util.Map)it).get("userId").equals(((java.util.Map)rr2.getBody().data()).get("id")));
        Assertions.assertTrue(found);
    }
}