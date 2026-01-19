package com.example.hungdt2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private int expMinutes = 60;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public int getExpMinutes() { return expMinutes; }
    public void setExpMinutes(int expMinutes) { this.expMinutes = expMinutes; }
}
