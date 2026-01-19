package com.example.hungdt2.voice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AgoraTokenServiceTest {

    @Autowired
    AgoraTokenService tokenService;

    @Test
    public void tokenIsGenerated() {
        // If env or properties are not set, test still should pass (stub token).
        String t = tokenService.generateToken(1L, 2L, 300);
        Assertions.assertNotNull(t);
        Assertions.assertFalse(t.isBlank());
    }

    @Test
    public void tokenWithCredentials() throws Exception {
        // Set private fields on the bean instance to simulate provided credentials (do not commit secrets)
        java.lang.reflect.Field f1 = AgoraTokenService.class.getDeclaredField("appId");
        java.lang.reflect.Field f2 = AgoraTokenService.class.getDeclaredField("appCertificate");
        f1.setAccessible(true); f2.setAccessible(true);
        f1.set(tokenService, "fe50b2f38d16419f96e39014800557bb");
        f2.set(tokenService, "6c07f7860f3a49f180847b9fb65acd8c");

        String t = tokenService.generateToken(10L, 20L, 300);
        Assertions.assertNotNull(t);
        Assertions.assertFalse(t.isBlank());
        // Should not be the simple stub token when credentials are present (should be an Agora-style token)
        Assertions.assertFalse(t.startsWith("stub-"));
        // Preferably starts with Agora token prefix '007'
        Assertions.assertTrue(t.startsWith("007"), () -> "Expected token to start with '007', got: " + t);
    }
}