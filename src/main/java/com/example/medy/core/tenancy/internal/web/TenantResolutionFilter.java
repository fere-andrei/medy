package com.example.medy.core.tenancy.internal.web;

import com.example.medy.core.tenancy.TenantContext;
import com.example.medy.core.tenancy.internal.repository.OrganizationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Resolves the current tenant from the {@code X-Tenant-Id} header and
 * populates {@link TenantContext} for the duration of the request.
 * Temporary resolution strategy — will be replaced by extraction from the
 * authenticated JWT once auth exists, without downstream code changing.
 */
@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final OrganizationRepository organizationRepository;

    public TenantResolutionFilter(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(TENANT_HEADER);

        if (header != null) {
            UUID tenantId;
            try {
                tenantId = UUID.fromString(header);
            } catch (IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + TENANT_HEADER + " header");
                return;
            }

            if (!organizationRepository.existsById(tenantId)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown tenant");
                return;
            }

            TenantContext.setCurrentTenant(tenantId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
