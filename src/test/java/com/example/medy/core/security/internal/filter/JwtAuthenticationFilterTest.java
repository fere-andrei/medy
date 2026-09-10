package com.example.medy.core.security.internal.filter;

import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.enums.Role;
import com.example.medy.core.security.internal.jwt.JwtPrincipal;
import com.example.medy.core.security.internal.jwt.JwtService;
import com.example.medy.core.security.internal.repository.UserRepository;
import com.example.medy.core.tenancy.TenantContext;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userRepository);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeader_proceedsWithoutAuthenticating() throws Exception {
        when(request.getHeader(AUTH_HEADER)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userRepository);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void nonBearerAuthorizationHeader_proceedsWithoutAuthenticating() throws Exception {
        when(request.getHeader(AUTH_HEADER)).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    void validToken_setsAuthenticationAndTenantDuringChain_thenClearsAfter() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, tenantId, Role.CLINIC_ADMIN);
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer valid-token");
        when(jwtService.parseToken("valid-token")).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, tenantId, Role.CLINIC_ADMIN)));

        UUID[] observedTenant = new UUID[1];
        Authentication[] observedAuth = new Authentication[1];
        doAnswer(_ -> {
            observedTenant[0] = TenantContext.getCurrentTenant();
            observedAuth[0] = SecurityContextHolder.getContext().getAuthentication();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertThat(observedTenant[0]).isEqualTo(tenantId);
        assertThat(observedAuth[0].getPrincipal()).isEqualTo(principal);
        assertThat(observedAuth[0].getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_CLINIC_ADMIN");

        assertThat(TenantContext.getCurrentTenant()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void malformedToken_proceedsWithoutAuthenticating_andNeverSetsTenantContext() throws Exception {
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer garbage");
        when(jwtService.parseToken("garbage")).thenThrow(new MalformedJwtException("bad token"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void tokenParsingThrowsIllegalArgument_proceedsWithoutAuthenticating() throws Exception {
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer not-even-uuid-claims");
        when(jwtService.parseToken("not-even-uuid-claims")).thenThrow(new IllegalArgumentException("bad claim"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deactivatedUser_isNotAuthenticated_evenWithAnUnexpiredToken() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, UUID.randomUUID(), Role.DOCTOR);
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer valid-token");
        when(jwtService.parseToken("valid-token")).thenReturn(principal);
        User deactivated = activeUser(userId, principal.tenantId(), Role.DOCTOR);
        deactivated.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(deactivated));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void deletedUser_isNotAuthenticated() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, UUID.randomUUID(), Role.DOCTOR);
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer valid-token");
        when(jwtService.parseToken("valid-token")).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void roleChangedSinceTokenWasIssued_usesTheLiveRole_notTheStaleClaim() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        JwtPrincipal staleTokenClaims = new JwtPrincipal(userId, tenantId, Role.DOCTOR);
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer valid-token");
        when(jwtService.parseToken("valid-token")).thenReturn(staleTokenClaims);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, tenantId, Role.CLINIC_ADMIN)));

        Authentication[] observedAuth = new Authentication[1];
        doAnswer(_ -> {
            observedAuth[0] = SecurityContextHolder.getContext().getAuthentication();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertThat(observedAuth[0].getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_CLINIC_ADMIN");
    }

    @Test
    void superAdminWithTenantHeader_actsOnTheRequestedTenant() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID targetTenant = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, null, Role.SUPER_ADMIN);
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer valid-token");
        when(request.getHeader(TENANT_HEADER)).thenReturn(targetTenant.toString());
        when(jwtService.parseToken("valid-token")).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, null, Role.SUPER_ADMIN)));

        UUID[] observedTenant = new UUID[1];
        doAnswer(_ -> {
            observedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertThat(observedTenant[0]).isEqualTo(targetTenant);
    }

    @Test
    void superAdminWithoutTenantHeader_leavesTenantContextUnset() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, null, Role.SUPER_ADMIN);
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer valid-token");
        when(request.getHeader(TENANT_HEADER)).thenReturn(null);
        when(jwtService.parseToken("valid-token")).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, null, Role.SUPER_ADMIN)));

        UUID[] observedTenant = new UUID[]{UUID.randomUUID()};
        doAnswer(_ -> {
            observedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertThat(observedTenant[0]).isNull();
    }

    @Test
    void superAdminWithMalformedTenantHeader_leavesTenantContextUnset() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, null, Role.SUPER_ADMIN);
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer valid-token");
        when(request.getHeader(TENANT_HEADER)).thenReturn("not-a-uuid");
        when(jwtService.parseToken("valid-token")).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, null, Role.SUPER_ADMIN)));

        UUID[] observedTenant = new UUID[]{UUID.randomUUID()};
        doAnswer(_ -> {
            observedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertThat(observedTenant[0]).isNull();
    }

    @Test
    void nonSuperAdminWithTenantHeader_ignoresHeader_keepsOwnTenant() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID ownTenant = UUID.randomUUID();
        UUID someoneElsesTenant = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, ownTenant, Role.CLINIC_ADMIN);
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer valid-token");
        lenient().when(request.getHeader(TENANT_HEADER)).thenReturn(someoneElsesTenant.toString());
        when(jwtService.parseToken("valid-token")).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, ownTenant, Role.CLINIC_ADMIN)));

        UUID[] observedTenant = new UUID[1];
        doAnswer(_ -> {
            observedTenant[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertThat(observedTenant[0]).isEqualTo(ownTenant);
    }

    private User activeUser(UUID id, UUID tenantId, Role role) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
