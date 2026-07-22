package com.von.gateway.filter;

import com.von.common.api.ApiErrorCode;
import com.von.common.enums.UserRole;
import com.von.common.security.JwtConstants;
import com.von.common.security.JwtUtils;
import com.von.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;

    public JwtAuthFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (isPublic(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (path.startsWith("/api/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = JwtUtils.extractBearerToken(request.getHeader(JwtConstants.HEADER_AUTHORIZATION));
        if (token == null) {
            writeUnauthorized(response, "缺少 Authorization Bearer Token");
            return;
        }

        try {
            Claims claims = JwtUtils.parseToken(token, jwtProperties.getSecret());
            Long userId = JwtUtils.getUserId(claims);
            String role = JwtUtils.getRole(claims).name();
            if (path.startsWith("/api/admin/") && !path.startsWith("/api/admin/auth/")
                    && !UserRole.ADMIN.name().equals(role)) {
                writeForbidden(response, "需要管理员权限");
                return;
            }
            filterChain.doFilter(new HeaderInjectRequest(request, userId, role), response);
        } catch (Exception ex) {
            writeUnauthorized(response, "Token 无效或已过期");
        }
    }

    private boolean isPublic(String path) {
        if (path.startsWith("/actuator")) {
            return true;
        }
        if (path.startsWith("/api/map/")) {
            return true;
        }
        for (String publicPath : jwtProperties.getPublicPaths()) {
            if (path.equals(publicPath) || path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, message);
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, ApiErrorCode.FORBIDDEN, message);
    }

    private void writeJsonError(HttpServletResponse response, int status, String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"success":"false","errorCode":"%s","message":"%s"}
                """.formatted(errorCode, message));
    }

    private static class HeaderInjectRequest extends HttpServletRequestWrapper {
        private final Map<String, String> extraHeaders = new HashMap<>();

        HeaderInjectRequest(HttpServletRequest request, Long userId, String role) {
            super(request);
            extraHeaders.put(JwtConstants.HEADER_USER_ID, String.valueOf(userId));
            extraHeaders.put(JwtConstants.HEADER_USER_ROLE, role);
        }

        @Override
        public String getHeader(String name) {
            if (extraHeaders.containsKey(name)) {
                return extraHeaders.get(name);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new HashSet<>(extraHeaders.keySet());
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                names.add(original.nextElement());
            }
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (extraHeaders.containsKey(name)) {
                return Collections.enumeration(List.of(extraHeaders.get(name)));
            }
            return super.getHeaders(name);
        }
    }
}
