package com.example.medy.core.security.internal.config;

import com.example.medy.core.licensing.internal.enums.ModuleCode;
import com.example.medy.core.security.internal.enums.Role;

/** One row of {@code SecurityConfig}'s access policy: which roles, gated by which module entitlement, on which URL. */
record ModuleAccessRule(String urlPattern, ModuleCode moduleCode, Role... roles) {
}
