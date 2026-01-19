package com.example.hungdt2.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        String tokenCandidate = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenCandidate = auth.substring(7);
        } else {
            // also accept token via query param as fallback (access_token or token)
            String p1 = request.getParameter("access_token");
            String p2 = request.getParameter("token");
            if (p1 != null && !p1.isBlank()) tokenCandidate = p1;
            else if (p2 != null && !p2.isBlank()) tokenCandidate = p2;
            if (tokenCandidate != null && tokenCandidate.startsWith("Bearer ")) tokenCandidate = tokenCandidate.substring(7);
        }

        if (tokenCandidate != null) {
            try {
                Jws<Claims> claims = jwtService.parseToken(tokenCandidate);
                String subject = claims.getBody().getSubject();
                Long userId = Long.valueOf(subject);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                // invalid token -> reject with 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":{\"code\":\"INVALID_TOKEN\",\"message\":\"Invalid or expired token\"}}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
