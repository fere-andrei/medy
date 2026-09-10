package com.example.medy.core.security.internal.filter;

import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.enums.Role;
import com.example.medy.core.security.internal.jwt.JwtPrincipal;
import com.example.medy.core.security.internal.jwt.JwtService;
import com.example.medy.core.security.internal.repository.UserRepository;
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
 * The JWT only proves "this user id authenticated successfully at issue
 * time" — it does NOT get trusted for role/tenant/active-status, all of
 * which are re-read from the database on every request via {@code userId}.
 * This is deliberately a per-request DB read, not just a cache of the JWT's
 * claims: a deactivated account, or one whose role changed mid-session, must
 * lose access immediately rather than waiting out the token's remaining
 * lifetime (see {@link JwtService}'s expiration).
 * <p>
 * The one exception is {@link #TENANT_OVERRIDE_HEADER}, honored only for
 * {@link Role#SUPER_ADMIN}. That role has no home tenant (see
 * {@link JwtPrincipal}), so a tenant-scoped endpoint is otherwise meaningless
 * for it — there's no tenant to act on. Every other role's tenant still comes
 * solely from the user's own row; this header is inert for them. An invalid
 * or non-existent id here isn't validated up front — it simply resolves to
 * zero rows and no module entitlement downstream, the same as any tenant the
 * caller isn't authorized for, so there's nothing to leak by skipping the
 * check.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TENANT_OVERRIDE_HEADER = "X-Tenant-Id";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                JwtPrincipal tokenPrincipal = jwtService.parseToken(token);
                userRepository.findById(tokenPrincipal.userId())
                        .filter(User::isActive)
                        .ifPresent(user -> authenticate(request, user));
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

    private void authenticate(HttpServletRequest request, User user) {
        JwtPrincipal livePrincipal = new JwtPrincipal(user.getId(), user.getTenantId(), user.getRole());
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        Authentication authentication = new UsernamePasswordAuthenticationToken(livePrincipal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (user.getTenantId() != null) {
            TenantContext.setCurrentTenant(user.getTenantId());
        } else if (user.getRole() == Role.SUPER_ADMIN) {
            actingTenantFromHeader(request).ifPresent(TenantContext::setCurrentTenant);
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
