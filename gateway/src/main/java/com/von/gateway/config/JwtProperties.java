package com.von.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret = "CHANGE_ME_JWT_SECRET";
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/api/passenger/auth/login",
            "/api/driver/auth/login",
            "/api/admin/auth/login"
    ));

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}
