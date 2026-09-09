package com.example.medy.core.security.internal.config;

import com.example.medy.core.licensing.internal.enums.ModuleCode;
import com.example.medy.core.licensing.internal.repository.TenantModuleEntitlementRepository;
import com.example.medy.core.licensing.internal.security.ModuleEntitlementAuthorizationManager;
import com.example.medy.core.security.internal.enums.Role;
import com.example.medy.core.security.internal.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableMethodSecurity
class SecurityConfig {

    private static final Role[] STAFF_ROLES =
            {Role.SUPER_ADMIN, Role.CLINIC_ADMIN, Role.DOCTOR, Role.RECEPTIONIST, Role.ASSISTANT};

    /**
     * One row per gated module — adding module #10 is a one-line addition
     * here, nothing else in this class changes. If a module ever needs a
     * different role set, just pass different roles for that row.
     */
    private static final List<ModuleAccessRule> MODULE_ACCESS_RULES = List.of(
            new ModuleAccessRule("/patients/**", ModuleCode.PATIENT_MANAGEMENT, STAFF_ROLES),
            new ModuleAccessRule("/appointments/**", ModuleCode.APPOINTMENTS, STAFF_ROLES));

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantModuleEntitlementRepository entitlementRepository;

    SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            TenantModuleEntitlementRepository entitlementRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.entitlementRepository = entitlementRepository;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // "/error" must stay open: Spring Boot's default error handling
                    // internally forwards here to render any error response (4xx/5xx),
                    // and that forward re-enters this same filter chain as a fresh,
                    // unauthenticated request — blocking it would clobber every error
                    // body (including this controller's own 401s) with a generic 403.
                    auth.requestMatchers("/auth/**", "/error").permitAll();

                    for (ModuleAccessRule rule : MODULE_ACCESS_RULES) {
                        auth.requestMatchers(rule.urlPattern()).access(AuthorizationManagers.allOf(
                                AuthorityAuthorizationManager.hasAnyRole(roleNames(rule.roles())),
                                new ModuleEntitlementAuthorizationManager(rule.moduleCode(), entitlementRepository)));
                    }

                    // Deny by default: any new controller must get an explicit rule
                    // above (permitAll, a ModuleAccessRule, or its own matcher) or
                    // it's unreachable, rather than silently inheriting open access.
                    auth.anyRequest().denyAll();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static String[] roleNames(Role[] roles) {
        return Stream.of(roles).map(Role::name).toArray(String[]::new);
    }

}
