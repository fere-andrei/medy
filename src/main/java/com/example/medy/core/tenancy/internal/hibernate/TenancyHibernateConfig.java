package com.example.medy.core.tenancy.internal.hibernate;

import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TenancyHibernateConfig {

    @Bean
    HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer(TenantIdentifierResolver resolver) {
        return properties -> properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
