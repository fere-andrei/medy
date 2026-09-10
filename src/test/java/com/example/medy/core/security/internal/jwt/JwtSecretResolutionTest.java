package com.example.medy.core.security.internal.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the real application.properties / application-prod.properties files
 * (not a copy of their content) to prove the 'prod' profile's
 * {@code app.jwt.secret=${JWT_SECRET}} genuinely has no fallback — unlike a
 * hand-written check, this fails the moment someone edits that placeholder
 * back into having a default.
 * <p>
 * {@code PropertyPlaceholderAutoConfiguration} has to be pulled in explicitly:
 * a bare {@code ApplicationContextRunner} doesn't auto-configure anything, so
 * without it a nested {@code ${...}} inside a property value is never even
 * resolved/validated — a real Spring Boot app always has it.
 */
class JwtSecretResolutionTest {

    @Configuration
    static class RequiresJwtSecret {

        RequiresJwtSecret(@Value("${app.jwt.secret}") String secret) {
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(RequiresJwtSecret.class);

    @Test
    void prodProfileWithoutJwtSecretEnvVar_failsToStart() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void prodProfileWithJwtSecretEnvVar_startsFine() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod", "JWT_SECRET=a-real-secret-that-was-not-committed")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void defaultProfile_usesTheDevSecretWithoutRequiringAnEnvVar() {
        contextRunner.run(context -> assertThat(context).hasNotFailed());
    }
}
