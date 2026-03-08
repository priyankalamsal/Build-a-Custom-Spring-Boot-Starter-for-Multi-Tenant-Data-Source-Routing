package com.example.multitenancy.starter;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TenantDataSourceRegistry {
    private final Map<String, DataSource> tenantDataSources;

    public TenantDataSourceRegistry(Map<String, DataSource> tenantDataSources) {
        if (tenantDataSources == null || tenantDataSources.isEmpty()) {
            throw new IllegalArgumentException("Tenant data sources must be provided.");
        }
        this.tenantDataSources = Collections.unmodifiableMap(new LinkedHashMap<>(tenantDataSources));
    }

    public Map<String, DataSource> getTenantDataSources() {
        return tenantDataSources;
    }

    public boolean hasTenant(String tenantId) {
        return tenantDataSources.containsKey(tenantId);
    }
}
