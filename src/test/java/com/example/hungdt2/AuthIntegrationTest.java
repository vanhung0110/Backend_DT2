package com.example.hungdt2;

import com.example.hungdt2.auth.dto.LoginRequest;
import com.example.hungdt2.auth.dto.RegisterRequest;
import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.user.dto.UserMeResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    private org.springframework.web.client.RestTemplate rest;

    @Test
    public void fullFlowRegisterLoginMe() {
        // initialize simple RestTemplate and set base URL manually (avoid TestRestTemplate classloader issues)
        this.rest = new org.springframework.web.client.RestTemplate();
        String base = "http://localhost:" + port;

        RegisterRequest reg = new RegisterRequest("namnguyen","nam@gmail.com","P@ssw0rd123","0909","Nam Nguyen");
        ResponseEntity<ApiResponse> r1 = rest.postForEntity(base + "/auth/register", reg, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, r1.getStatusCode());

        LoginRequest login = new LoginRequest("namnguyen","P@ssw0rd123");
        ResponseEntity<ApiResponse> r2 = rest.postForEntity(base + "/auth/login", login, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r2.getStatusCode());
        // extract token
        Object data = ((java.util.Map)r2.getBody().data()).get("accessToken");
        Assertions.assertNotNull(data);
        String token = data.toString();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<ApiResponse> r3 = rest.exchange(base + "/users/me", HttpMethod.GET, entity, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r3.getStatusCode());
    }
}
