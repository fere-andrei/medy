/**
 * Shared kernel — deliberately OPEN: every sub-package here (tenancy,
 * security, web, and future ones) is meant to be usable by every other
 * module, so nothing under {@code core} needs its own
 * {@code @NamedInterface} declaration.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.example.medy.core;
