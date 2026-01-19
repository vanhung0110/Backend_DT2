package com.example.hungdt2.websocket;

import com.example.hungdt2.auth.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class TokenHandshakeInterceptorTest {

    @Autowired
    private TokenHandshakeInterceptor interceptor;

    @Autowired
    private JwtService jwtService;

    @Test
    public void missingTokenRejected() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/ws/rooms/1");
        ServletServerHttpRequest request = new ServletServerHttpRequest(req);
        MockHttpServletResponse res = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(res);
        Map<String, Object> attrs = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(request, response, null, attrs);
        Assertions.assertFalse(ok);
        Assertions.assertEquals(401, res.getStatus());
    }

    @Test
    public void invalidTokenRejected() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/ws/rooms/1");
        req.setQueryString("token=badtoken");
        ServletServerHttpRequest request = new ServletServerHttpRequest(req);
        MockHttpServletResponse res = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(res);
        Map<String, Object> attrs = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(request, response, null, attrs);
        Assertions.assertFalse(ok);
        Assertions.assertEquals(401, res.getStatus());
    }

    @Test
    public void validTokenAccepted() throws Exception {
        String token = jwtService.generateToken(42L);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/ws/rooms/1");
        req.setQueryString("token=" + token);
        ServletServerHttpRequest request = new ServletServerHttpRequest(req);
        MockHttpServletResponse res = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(res);
        Map<String, Object> attrs = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(request, response, null, attrs);
        Assertions.assertTrue(ok);
        Assertions.assertEquals(42L, attrs.get("userId"));
    }
}
