package com.example.hungdt2.auth;

import com.example.hungdt2.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final Key key;
    private final int expMinutes;

    public JwtService(JwtProperties props) {
        byte[] secret = props.getSecret() == null ? "change-me-in-production".getBytes() : props.getSecret().getBytes();
        this.key = Keys.hmacShaKeyFor(secret);
        this.expMinutes = props.getExpMinutes();
    }

    public String generateToken(Long userId) {
        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plusSeconds(expMinutes * 60L));
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(iat)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseToken(String token) throws JwtException {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
