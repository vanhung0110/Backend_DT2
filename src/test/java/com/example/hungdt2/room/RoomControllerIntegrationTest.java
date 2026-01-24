package com.example.hungdt2.room;

import com.example.hungdt2.auth.dto.LoginRequest;
import com.example.hungdt2.auth.dto.RegisterRequest;
import com.example.hungdt2.common.ApiResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RoomControllerIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    @Test
    public void creatingDirectRoomEnablesVoiceByDefault() throws Exception {
        var rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;
        String suffix = String.valueOf(System.currentTimeMillis());
        RegisterRequest r1 = new RegisterRequest("owner"+suffix,"owner"+suffix+"@local","P@ssw0rd123","09990050","Owner");
        ResponseEntity<ApiResponse> rr1 = rest.postForEntity(base + "/auth/register", r1, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr1.getStatusCode());
        RegisterRequest r2 = new RegisterRequest("other"+suffix,"other"+suffix+"@local","P@ssw0rd123","09990051","User");
        ResponseEntity<ApiResponse> rr2 = rest.postForEntity(base + "/auth/register", r2, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, rr2.getStatusCode());

        ResponseEntity<ApiResponse> login1 = rest.postForEntity(base + "/auth/login", new LoginRequest("owner"+suffix,"P@ssw0rd123"), ApiResponse.class);
        String token1 = (String) ((java.util.Map)login1.getBody().data()).get("accessToken");

        Integer otherIdInt = (Integer) ((java.util.Map) rr2.getBody().data()).get("id");
        Long otherId = otherIdInt.longValue();

        HttpHeaders headers = new HttpHeaders(); headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token1);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<ApiResponse> resp = rest.postForEntity(base + "/rooms/direct/" + otherId, entity, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        java.util.Map data = (java.util.Map) resp.getBody().data();
        Assertions.assertTrue(data.containsKey("voiceEnabled"));
        Assertions.assertEquals(Boolean.TRUE, data.get("voiceEnabled"));
    }
}
