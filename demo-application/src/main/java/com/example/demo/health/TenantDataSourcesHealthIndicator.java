package com.example.demo.health;

import com.example.multitenancy.starter.TenantDataSourceRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component("datasources")
public class TenantDataSourcesHealthIndicator implements HealthIndicator {
    private final TenantDataSourceRegistry registry;

    public TenantDataSourcesHealthIndicator(TenantDataSourceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Health health() {
        Map<String, Object> tenantStatus = new LinkedHashMap<>();
        boolean allHealthy = true;
        for (Map.Entry<String, DataSource> entry : registry.getTenantDataSources().entrySet()) {
            Map<String, Object> details = new LinkedHashMap<>();
            try (Connection connection = entry.getValue().getConnection()) {
                details.put("status", "UP");
                details.put("url", connection.getMetaData().getURL());
            } catch (SQLException ex) {
                allHealthy = false;
                details.put("status", "DOWN");
                details.put("error", ex.getMessage());
            }
            tenantStatus.put(entry.getKey(), details);
        }
        return (allHealthy ? Health.up() : Health.down())
                .withDetail("components", tenantStatus)
                .build();
    }
}
