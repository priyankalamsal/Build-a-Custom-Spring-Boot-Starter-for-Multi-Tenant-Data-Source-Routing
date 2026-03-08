package com.example.multitenancy.starter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface TenantResolver {
    Optional<String> resolveTenant(HttpServletRequest request);
}
