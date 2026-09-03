package com.example.medy.core.security.internal.jwt;

import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:480}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String issueToken(User user) {
        Instant now = Instant.now();

        JwtBuilder builder = Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)));

        if (user.getTenantId() != null) {
            builder.claim("tenantId", user.getTenantId().toString());
        }

        return builder.signWith(key).compact();
    }

    public JwtPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        Role role = Role.valueOf(claims.get("role", String.class));

        String tenantIdClaim = claims.get("tenantId", String.class);
        UUID tenantId = tenantIdClaim != null ? UUID.fromString(tenantIdClaim) : null;

        return new JwtPrincipal(userId, tenantId, role);
    }
}
