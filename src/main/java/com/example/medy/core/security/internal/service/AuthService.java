package com.example.medy.core.security.internal.service;

import com.example.medy.core.security.internal.dto.LoginRequestDTO;
import com.example.medy.core.security.internal.dto.LoginResponseDTO;
import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.jwt.JwtService;
import com.example.medy.core.security.internal.repository.UserRepository;
import com.example.medy.core.tenancy.internal.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Boundary that owns the {@code User} entity — the controller only ever sees
 * DTOs in and DTOs out.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        Optional<User> user = request.orgSlug() != null
                ? organizationRepository.findBySlug(request.orgSlug())
                        .flatMap(org -> userRepository.findByTenantIdAndEmail(org.getId(), request.email()))
                : userRepository.findByTenantIdIsNullAndEmail(request.email());

        User authenticated = user
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        return new LoginResponseDTO(jwtService.issueToken(authenticated));
    }
}
