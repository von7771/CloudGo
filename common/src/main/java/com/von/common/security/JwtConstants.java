package com.von.common.security;

/**
 * JWT 请求头与 Claim 字段名常量。
 */
public final class JwtConstants {

    private JwtConstants() {
    }

    /** HTTP 请求头：Authorization: Bearer &lt;token&gt; */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** Bearer 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    /** JWT claim：用户 ID */
    public static final String CLAIM_USER_ID = "userId";

    /** JWT claim：角色 PASSENGER / DRIVER / ADMIN */
    public static final String CLAIM_ROLE = "role";

    /** Gateway 转发给下游的内部 Header（可选） */
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
}
