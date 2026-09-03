package com.example.medy;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fails the build if any module reaches into another module's internals, or
 * uses a type from another module's root package that isn't declared via
 * {@code @NamedInterface} — the same check IntelliJ's live inspection does,
 * enforced here so it can't be missed outside the IDE.
 */
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(MedyApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
