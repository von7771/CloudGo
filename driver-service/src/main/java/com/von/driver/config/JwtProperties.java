package com.von.driver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret = "CHANGE_ME_JWT_SECRET";
    private long expireMs = 86400000L;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getExpireMs() { return expireMs; }
    public void setExpireMs(long expireMs) { this.expireMs = expireMs; }
}
