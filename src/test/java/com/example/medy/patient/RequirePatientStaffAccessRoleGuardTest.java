package com.example.medy.patient;

import com.example.medy.core.security.internal.enums.Role;
import com.example.medy.patient.internal.security.RequirePatientStaffAccess;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @PreAuthorize} SpEL strings can't reference the {@code Role} enum
 * directly (annotation values must be compile-time constants, and
 * {@code Role.X.name()} isn't one) — so a typo or a stale name after a rename
 * compiles fine and just silently stops matching anyone. This test catches
 * that class of mistake at test time instead.
 * <p>
 * Only one place needs checking now: {@link RequirePatientStaffAccess}'s own
 * meta-annotation — every caller uses that name, never a raw role string.
 */
class RequirePatientStaffAccessRoleGuardTest {

    private static final Pattern ROLE_NAME = Pattern.compile("'([A-Z_]+)'");

    @Test
    void requirePatientStaffAccess_onlyReferencesRealRoleEnumValues() {
        PreAuthorize preAuthorize = RequirePatientStaffAccess.class.getAnnotation(PreAuthorize.class);

        Matcher matcher = ROLE_NAME.matcher(preAuthorize.value());
        while (matcher.find()) {
            String roleName = matcher.group(1);
            assertThat(isRealRole(roleName))
                    .as("'%s' in @RequirePatientStaffAccess must match a real Role enum constant", roleName)
                    .isTrue();
        }
    }

    private boolean isRealRole(String name) {
        try {
            Role.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
