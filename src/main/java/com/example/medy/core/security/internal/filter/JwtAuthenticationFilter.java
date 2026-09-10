package com.example.medy.core.security.internal.filter;

import com.example.medy.core.security.internal.enums.Role;
import com.example.medy.core.security.internal.jwt.JwtPrincipal;
import com.example.medy.core.security.internal.jwt.JwtService;
import com.example.medy.core.tenancy.TenantContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Replaces the earlier header-based TenantResolutionFilter: the tenant is now
 * derived from a validated JWT instead of a trusted client-supplied header.
 * Also populates Spring Security's context so {@code @PreAuthorize} checks
 * work on the parsed role.
 * <p>
 * The one exception is {@link #TENANT_OVERRIDE_HEADER}, honored only for
 * {@link Role#SUPER_ADMIN}. That role has no home tenant (see
 * {@link JwtPrincipal}), so a tenant-scoped endpoint is otherwise meaningless
 * for it — there's no tenant to act on. Every other role's tenant still comes
 * solely from the JWT; this header is inert for them. An invalid or
 * non-existent id here isn't validated up front — it simply resolves to zero
 * rows and no module entitlement downstream, the same as any tenant the
 * caller isn't authorized for, so there's nothing to leak by skipping the
 * check.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TENANT_OVERRIDE_HEADER = "X-Tenant-Id";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                JwtPrincipal principal = jwtService.parseToken(token);

                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (principal.tenantId() != null) {
                    TenantContext.setCurrentTenant(principal.tenantId());
                } else if (principal.role() == Role.SUPER_ADMIN) {
                    actingTenantFromHeader(request).ifPresent(TenantContext::setCurrentTenant);
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Invalid/expired token: leave the request unauthenticated —
                // Spring Security's authorization rules reject it downstream.
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<UUID> actingTenantFromHeader(HttpServletRequest request) {
        String tenantHeader = request.getHeader(TENANT_OVERRIDE_HEADER);
        if (tenantHeader == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(tenantHeader));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
