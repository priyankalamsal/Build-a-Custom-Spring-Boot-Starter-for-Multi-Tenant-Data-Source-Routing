package com.example.multitenancy.starter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class TenantInterceptor implements HandlerInterceptor {
    private final TenantResolver tenantResolver;
    private final TenantDataSourceRegistry registry;

    public TenantInterceptor(TenantResolver tenantResolver, TenantDataSourceRegistry registry) {
        this.tenantResolver = tenantResolver;
        this.registry = registry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        Optional<String> tenantId = tenantResolver.resolveTenant(request);
        if (tenantId.isEmpty()) {
            sendError(response, HttpStatus.BAD_REQUEST, "X-Tenant-ID header is missing.");
            return false;
        }

        String candidate = tenantId.get();
        if (!registry.hasTenant(candidate)) {
            sendError(response, HttpStatus.NOT_FOUND, "Tenant not found: " + candidate);
            return false;
        }

        TenantContext.setCurrentTenant(candidate);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String sanitized = message.replace("\"", "\\\"");
        response.getWriter().append("{\"error\":\"").append(status.getReasonPhrase()).append("\",\"message\":\"").append(sanitized).append("\"}");
        response.flushBuffer();
    }
}
