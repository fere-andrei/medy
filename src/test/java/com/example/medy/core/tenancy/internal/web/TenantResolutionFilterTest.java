package com.example.medy.core.tenancy.internal.web;

import com.example.medy.core.tenancy.TenantContext;
import com.example.medy.core.tenancy.internal.repository.OrganizationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantResolutionFilterTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private TenantResolutionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantResolutionFilter(organizationRepository);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void noHeader_proceedsWithoutTouchingTenantContextOrRepository() throws Exception {
        when(request.getHeader(TENANT_HEADER)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(organizationRepository);
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void knownTenant_isVisibleDuringChainAndClearedAfterward() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(request.getHeader(TENANT_HEADER)).thenReturn(tenantId.toString());
        when(organizationRepository.existsById(tenantId)).thenReturn(true);

        UUID[] observedDuringChain = new UUID[1];
        doAnswer(invocation -> {
            observedDuringChain[0] = TenantContext.getCurrentTenant();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertThat(observedDuringChain[0]).isEqualTo(tenantId);
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void malformedHeader_rejectsWithBadRequestAndNeverReachesChain() throws Exception {
        when(request.getHeader(TENANT_HEADER)).thenReturn("not-a-uuid");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
        verifyNoInteractions(filterChain);
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void unknownTenant_rejectsWithNotFoundAndNeverReachesChain() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(request.getHeader(TENANT_HEADER)).thenReturn(tenantId.toString());
        when(organizationRepository.existsById(tenantId)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
        verifyNoInteractions(filterChain);
    }
}
