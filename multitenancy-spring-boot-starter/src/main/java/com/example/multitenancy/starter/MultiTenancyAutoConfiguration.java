package com.example.multitenancy.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(MultiTenancyProperties.class)
@ConditionalOnProperty(prefix = "multitenancy", name = "enabled", havingValue = "true")
public class MultiTenancyAutoConfiguration {

    @Bean
    public TenantResolver tenantResolver() {
        return new HeaderTenantResolver();
    }

    @Bean
    public TenantInterceptor tenantInterceptor(TenantResolver tenantResolver, TenantDataSourceRegistry registry) {
        return new TenantInterceptor(tenantResolver, registry);
    }

    @Bean
    public TenantDataSourceRegistry tenantDataSourceRegistry(MultiTenancyProperties properties) {
        return new TenantDataSourceRegistry(buildDataSources(properties));
    }

    @Bean
    @Primary
    public TenantAwareRoutingDataSource tenantAwareRoutingDataSource(TenantDataSourceRegistry registry) {
        Map<String, DataSource> sources = registry.getTenantDataSources();
        Map<Object, Object> lookup = new LinkedHashMap<>(sources);
        TenantAwareRoutingDataSource routingDataSource = new TenantAwareRoutingDataSource();
        routingDataSource.setTargetDataSources(lookup);
        routingDataSource.setDefaultTargetDataSource(lookup.values().iterator().next());
        routingDataSource.setLenientFallback(false);
        return routingDataSource;
    }

    @Bean
    public WebMvcConfigurer tenantInterceptorConfigurer(TenantInterceptor tenantInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(tenantInterceptor)
                        .order(Integer.MIN_VALUE)
                        .excludePathPatterns("/actuator/**", "/error");
            }
        };
    }

    private Map<String, DataSource> buildDataSources(MultiTenancyProperties properties) {
        if (properties.getTenants().isEmpty()) {
            throw new IllegalStateException("At least one tenant must be configured when multitenancy is enabled.");
        }
        Map<String, DataSource> dataSources = new LinkedHashMap<>();
        for (MultiTenancyProperties.Tenant tenant : properties.getTenants()) {
            validateTenant(tenant);
            DataSource dataSource = DataSourceBuilder.create()
                    .driverClassName(tenant.getDriverClassName())
                    .url(tenant.getUrl())
                    .username(tenant.getUsername())
                    .password(tenant.getPassword())
                    .build();
            dataSources.put(tenant.getId(), dataSource);
        }
        return dataSources;
    }

    private void validateTenant(MultiTenancyProperties.Tenant tenant) {
        if (!isFilled(tenant.getId())) {
            throw new IllegalStateException("Tenant id must not be empty.");
        }
        if (!isFilled(tenant.getUrl())) {
            throw new IllegalStateException("Tenant url must not be empty for tenant " + tenant.getId());
        }
        if (!isFilled(tenant.getUsername())) {
            throw new IllegalStateException("Tenant username must not be empty for tenant " + tenant.getId());
        }
        if (!isFilled(tenant.getPassword())) {
            throw new IllegalStateException("Tenant password must not be empty for tenant " + tenant.getId());
        }
    }

    private boolean isFilled(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
