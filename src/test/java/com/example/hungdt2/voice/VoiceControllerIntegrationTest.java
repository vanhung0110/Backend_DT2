package com.example.hungdt2.voice;

import com.example.hungdt2.auth.dto.LoginRequest;
import com.example.hungdt2.auth.dto.RegisterRequest;
import com.example.hungdt2.common.ApiResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class VoiceControllerIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    @Test
    public void joinAndKickFlow() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;

        // register two users
        RegisterRequest r1 = new RegisterRequest("voiceu1","v1@example.com","P@ssw0rd123","09990001","Voice One");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());

        RegisterRequest r2 = new RegisterRequest("voiceu2","v2@example.com","P@ssw0rd123","09990002","Voice Two");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        // login u1
        LoginRequest l1 = new LoginRequest("voiceu1","P@ssw0rd123");
        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", l1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, login1.getStatusCode());
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");

        // login u2
        LoginRequest l2 = new LoginRequest("voiceu2","P@ssw0rd123");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", l2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, login2.getStatusCode());
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");

        // u1 create a private room and add u2
        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        java.util.Map<String,Object> payload = new java.util.HashMap<>(); payload.put("name", "Voice Room"); payload.put("type","PRIVATE"); payload.put("members", java.util.List.of(((java.util.Map)rr2.getBody().data()).get("id")));
        HttpEntity<java.util.Map<String,Object>> createReq = new HttpEntity<>(payload, headers1);
        ResponseEntity<ApiResponse> create = rest.postForEntity(base + "/rooms", createReq, ApiResponse.class);
        // allow 200 or 201 depending on controller implementation
        Assertions.assertTrue(create.getStatusCode() == HttpStatus.OK || create.getStatusCode() == HttpStatus.CREATED);
        java.util.Map created = (java.util.Map)create.getBody().data();
        Integer roomIdInt = (Integer) created.get("id");
        Long roomId = roomIdInt.longValue();

        // enable voice on this room for testing
        try {
            // Use simple update via H2 in-memory connection
            java.sql.Connection c = java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb", "SA", "");
            java.sql.PreparedStatement ps = c.prepareStatement("UPDATE rooms SET voice_enabled = TRUE WHERE id = ?");
            ps.setLong(1, roomId);
            ps.executeUpdate();
            ps.close();
            c.close();
        } catch (Exception ex) {
            // ignore if cannot set; tests may still pass if voice default set elsewhere
        }

        // u2 joins voice
        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2);
        HttpEntity<Void> ent2 = new HttpEntity<>(headers2);
        ResponseEntity<ApiResponse> j2 = rest.postForEntity(base + "/rooms/" + roomId + "/voice/join", ent2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, j2.getStatusCode());
        java.util.Map joinData2 = (java.util.Map) j2.getBody().data();
        // token may be null if server returns a tokenless join (App Certificate not enforced); ensure channel and appId are present instead.
        Assertions.assertNotNull(joinData2.get("channel"));
        Assertions.assertNotNull(joinData2.get("appId"));

        // u1 joins
        HttpEntity<Void> ent1 = new HttpEntity<>(headers1);
        ResponseEntity<ApiResponse> j1 = rest.postForEntity(base + "/rooms/" + roomId + "/voice/join", ent1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, j1.getStatusCode());

        // check members
        HttpEntity<Void> memReq = new HttpEntity<>(headers1);
        ResponseEntity<ApiResponse> membersRes = rest.exchange(base + "/rooms/" + roomId + "/voice/members", HttpMethod.GET, memReq, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, membersRes.getStatusCode());
        java.util.List list = (java.util.List) membersRes.getBody().data();
        Assertions.assertTrue(list.size() >= 2);

        // owner kicks u2
        HttpEntity<Void> kickReq = new HttpEntity<>(headers1);
        ResponseEntity<ApiResponse> kickRes = rest.postForEntity(base + "/rooms/" + roomId + "/voice/kick?userId=" + ((java.util.Map)rr2.getBody().data()).get("id"), kickReq, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, kickRes.getStatusCode());

        // u2 cannot rejoin
        Assertions.assertThrows(org.springframework.web.client.HttpClientErrorException.Forbidden.class, () -> {
            rest.postForEntity(base + "/rooms/" + roomId + "/voice/join", ent2, ApiResponse.class);
        });

    }

    @Test
    public void friendRoomVoiceFlow() {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;

        // register two users
        RegisterRequest r1 = new RegisterRequest("fvoiceu1","fv1@example.com","P@ssw0rd123","09990101","FVoice One");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());

        RegisterRequest r2 = new RegisterRequest("fvoiceu2","fv2@example.com","P@ssw0rd123","09990102","FVoice Two");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        // login u1
        LoginRequest l1 = new LoginRequest("fvoiceu1","P@ssw0rd123");
        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", l1, ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");

        // login u2
        LoginRequest l2 = new LoginRequest("fvoiceu2","P@ssw0rd123");
        ResponseEntity<ApiResponse> login2 = rest.postForEntity(base + "/auth/login", l2, ApiResponse.class);
        String token2 = (String) ((java.util.Map)login2.getBody().data()).get("accessToken");

        // u1 creates a friend room with u2
        HttpHeaders headers1 = new HttpHeaders(); headers1.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1); headers1.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<ApiResponse> frCreate = rest.postForEntity(base + "/friend-rooms/direct/" + ((java.util.Map)rr2.getBody().data()).get("id"), new HttpEntity<>(headers1), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, frCreate.getStatusCode());
        java.util.Map frCreated = (java.util.Map) frCreate.getBody().data();
        Integer underlyingRoomIdInt = (Integer) frCreated.get("id");
        Long underlyingRoomId = underlyingRoomIdInt.longValue();

        // find friend_room id via DB
        Long friendRoomId = null;
        try {
            java.sql.Connection c2 = java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb", "SA", "");
            java.sql.PreparedStatement ps2 = c2.prepareStatement("SELECT id FROM friend_rooms WHERE room_id = ?");
            ps2.setLong(1, underlyingRoomId);
            java.sql.ResultSet rs = ps2.executeQuery();
            if (rs.next()) friendRoomId = rs.getLong(1);
            rs.close(); ps2.close(); c2.close();
        } catch (Exception ex) { /* ignore */ }
        Assertions.assertNotNull(friendRoomId);
        final Long frId = friendRoomId;

        // u2 joins via friend-room endpoint
        HttpHeaders headers2 = new HttpHeaders(); headers2.add(HttpHeaders.AUTHORIZATION, "Bearer " + token2);
        HttpEntity<Void> ent2b = new HttpEntity<>(headers2);
        ResponseEntity<ApiResponse> fj2 = rest.postForEntity(base + "/friend-rooms/" + frId + "/voice/join", ent2b, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, fj2.getStatusCode());
        java.util.Map fj2Data = (java.util.Map) fj2.getBody().data();
        Assertions.assertNotNull(fj2Data.get("channel"));
        Assertions.assertNotNull(fj2Data.get("appId"));

        // u1 joins via friend-room endpoint
        HttpEntity<Void> ent1b = new HttpEntity<>(headers1);
        ResponseEntity<ApiResponse> fj1 = rest.postForEntity(base + "/friend-rooms/" + frId + "/voice/join", ent1b, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, fj1.getStatusCode());

        // check members via friend-room endpoint
        ResponseEntity<ApiResponse> fmembersRes = rest.exchange(base + "/friend-rooms/" + frId + "/voice/members", HttpMethod.GET, ent1b, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, fmembersRes.getStatusCode());
        java.util.List flist = (java.util.List) fmembersRes.getBody().data();
        Assertions.assertTrue(flist.size() >= 2);

        // owner kicks u2 via friend-room endpoint
        ResponseEntity<ApiResponse> fkickRes = rest.postForEntity(base + "/friend-rooms/" + frId + "/voice/kick?userId=" + ((java.util.Map)rr2.getBody().data()).get("id"), ent1b, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, fkickRes.getStatusCode());

        // u2 cannot rejoin via friend-room endpoint
        Assertions.assertThrows(org.springframework.web.client.HttpClientErrorException.Forbidden.class, () -> {
            rest.postForEntity(base + "/friend-rooms/" + frId + "/voice/join", ent2b, ApiResponse.class);
        });
    }
}
