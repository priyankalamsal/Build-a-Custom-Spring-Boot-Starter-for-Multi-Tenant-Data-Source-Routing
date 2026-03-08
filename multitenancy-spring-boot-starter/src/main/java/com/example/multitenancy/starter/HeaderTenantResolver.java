package com.example.multitenancy.starter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public class HeaderTenantResolver implements TenantResolver {
    public static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public Optional<String> resolveTenant(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        return Optional.ofNullable(header).map(String::trim).filter(value -> !value.isEmpty());
    }
}
