package com.von.common.security;

import com.von.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与解析工具（无 Spring 依赖，各服务/Gateway 均可使用）。
 */
public final class JwtUtils {

    private JwtUtils() {
    }

    /**
     * @param userId     用户 ID
     * @param role       角色
     * @param secret     密钥（至少 32 字符）
     * @param expireMs   过期毫秒数
     */
    public static String generateToken(Long userId, UserRole role, String secret, long expireMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(JwtConstants.CLAIM_USER_ID, userId)
                .claim(JwtConstants.CLAIM_ROLE, role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey(secret))
                .compact();
    }

    public static Claims parseToken(String token, String secret) {
        return Jwts.parser()
                .verifyWith(secretKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getUserId(Claims claims) {
        Object userId = claims.get(JwtConstants.CLAIM_USER_ID);
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(claims.getSubject());
    }

    public static UserRole getRole(Claims claims) {
        String role = claims.get(JwtConstants.CLAIM_ROLE, String.class);
        return UserRole.valueOf(role);
    }

    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(JwtConstants.BEARER_PREFIX.length()).trim();
    }

    private static SecretKey secretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
