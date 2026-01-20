package com.example.hungdt2;

import com.example.hungdt2.auth.dto.RegisterRequest;
import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.sms.DevSmsSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PasswordResetIntegrationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    private org.springframework.web.client.RestTemplate rest = new org.springframework.web.client.RestTemplate();

    @Test
    public void requestVerifyResetFlow() throws Exception {
        String base = "http://localhost:" + port;

        String phone = "0999000111";
        RegisterRequest reg = new RegisterRequest("u_for_reset","u_for_reset@example.com","InitialP@ss1",phone,"User Reset");
        var r1 = rest.postForEntity(base + "/auth/register", reg, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.CREATED, r1.getStatusCode());

        // request OTP
        var r2 = rest.postForEntity(base + "/auth/forgot/request-otp", java.util.Map.of("phone", phone), ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r2.getStatusCode());

        // grab OTP from DevSmsSender log cache
        String msg = DevSmsSender.getLastMessageForPhone(phone);
        Assertions.assertNotNull(msg, "No SMS captured for phone: " + phone);
        // extract 6-digit code from message
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{6})").matcher(msg);
        Assertions.assertTrue(m.find(), "No 6-digit OTP found in message: " + msg);
        String otp = m.group(1);

        // verify otp
        var verifyReq = java.util.Map.of("phone", phone, "otp", otp);
        var r3 = rest.postForEntity(base + "/auth/forgot/verify-otp", verifyReq, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r3.getStatusCode());
        // parse resetToken from response
        var data = (java.util.Map) r3.getBody().data();
        String resetToken = (String) data.get("resetToken");
        Assertions.assertNotNull(resetToken);

        // reset password
        var resetReq = java.util.Map.of("resetToken", resetToken, "password", "NewPass123");
        var r4 = rest.postForEntity(base + "/auth/forgot/reset", resetReq, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r4.getStatusCode());

        // login with new password
        var loginReq = new com.example.hungdt2.auth.dto.LoginRequest("u_for_reset", "NewPass123");
        var r5 = rest.postForEntity(base + "/auth/login", loginReq, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, r5.getStatusCode());
    }
}