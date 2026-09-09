package com.example.medy.core.security.internal.service;

import com.example.medy.core.security.internal.dto.RegisterStaffRequestDTO;
import com.example.medy.core.security.internal.dto.UserResponseDTO;
import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.enums.Role;
import com.example.medy.core.security.internal.mapper.UserMapper;
import com.example.medy.core.security.internal.repository.UserRepository;
import com.example.medy.core.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

/**
 * Owns creation of staff {@code User} accounts, invited by a
 * {@code CLINIC_ADMIN} into their own tenant. Kept separate from
 * {@link AuthService}: that class authenticates existing users, this one
 * provisions new ones — different responsibilities, different reasons to
 * change.
 */
@Service
public class UserService {

    /**
     * Roles a CLINIC_ADMIN may hand out through this endpoint. Deliberately
     * excludes CLINIC_ADMIN, ACCOUNTANT, and SUPER_ADMIN — granting a peer
     * admin, billing, or platform-level role is a separate, more sensitive
     * operation than inviting clinical staff and isn't allowed here.
     */
    private static final Set<Role> ASSIGNABLE_ROLES = Set.of(Role.DOCTOR, Role.RECEPTIONIST, Role.ASSISTANT);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO registerStaff(RegisterStaffRequestDTO request) {
        if (!ASSIGNABLE_ROLES.contains(request.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Role " + request.role() + " cannot be assigned through this endpoint");
        }

        UUID tenantId = TenantContext.getCurrentTenant();

        if (userRepository.findByTenantIdAndEmail(tenantId, request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered in this clinic");
        }

        User user = userMapper.toEntity(request);
        user.setTenantId(tenantId);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        return UserResponseDTO.from(userRepository.save(user));
    }
}
