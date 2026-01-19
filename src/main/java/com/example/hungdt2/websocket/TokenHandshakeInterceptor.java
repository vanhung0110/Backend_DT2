package com.example.hungdt2.websocket;

import com.example.hungdt2.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class TokenHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(TokenHandshakeInterceptor.class);

    private final JwtService jwtService;

    public TokenHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        log.info("WS handshake attempt: uri={} tokenPresent={}", request.getURI(), token != null && !token.isBlank());
        if (token == null || token.isBlank()) {
            log.warn("WS handshake rejected: missing token");
            try { response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED); } catch (Exception e) { }
            return false;
        }
        try {
            Jws<Claims> claims = jwtService.parseToken(token);
            String sub = claims.getBody().getSubject();
            Long userId = null;
            try { userId = Long.valueOf(sub); } catch (NumberFormatException ex) { /* ignore */ }
            if (userId == null) {
                log.warn("WS handshake rejected: token subject is not a user id");
                try { response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED); } catch (Exception e) { }
                return false;
            }
            attributes.put("userId", userId);
            log.info("WS handshake accepted for userId={}", userId);
            return true;
        } catch (JwtException ex) {
            log.warn("WS handshake rejected: invalid token: {}", ex.getMessage());
            try { response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED); } catch (Exception e) { }
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               org.springframework.web.socket.WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
