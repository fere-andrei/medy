package com.example.medy.core.security.internal.web;

import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.jwt.JwtService;
import com.example.medy.core.security.internal.repository.UserRepository;
import com.example.medy.core.tenancy.internal.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        Optional<User> user = request.orgSlug() != null
                ? organizationRepository.findBySlug(request.orgSlug())
                        .flatMap(org -> userRepository.findByTenantIdAndEmail(org.getId(), request.email()))
                : userRepository.findByTenantIdIsNullAndEmail(request.email());

        User authenticated = user
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        return new LoginResponse(jwtService.issueToken(authenticated));
    }
}
