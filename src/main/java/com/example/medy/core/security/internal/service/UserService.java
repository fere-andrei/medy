package com.example.medy.core.security.internal.service;

import com.example.medy.core.security.internal.dto.*;
import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.enums.Role;
import com.example.medy.core.security.internal.mapper.UserMapper;
import com.example.medy.core.security.internal.repository.UserRepository;
import com.example.medy.core.security.internal.repository.projection.RoleCount;
import com.example.medy.core.tenancy.TenantContext;
import com.example.medy.core.web.AuthenticationFailedException;
import com.example.medy.core.web.ForbiddenOperationException;
import com.example.medy.core.web.InvalidRequestException;
import com.example.medy.core.web.ResourceConflictException;
import com.example.medy.core.web.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        requireAssignableRole(request.role());

        UUID tenantId = TenantContext.getCurrentTenant();

        if (userRepository.findByTenantIdAndEmail(tenantId, request.email()).isPresent()) {
            throw new ResourceConflictException(SecurityMessages.EMAIL_ALREADY_REGISTERED);
        }

        User user = userMapper.toEntity(request);
        user.setTenantId(tenantId);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        return UserResponseDTO.from(userRepository.save(user));
    }

    public List<UserResponseDTO> list() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return userRepository.findAllByTenantId(tenantId).stream().map(UserResponseDTO::from).toList();
    }


    public UserSummaryResponseDTO getSummary() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return new UserSummaryResponseDTO(
                userRepository.countByTenantIdGroupedByRole(tenantId).stream()
                        .collect(Collectors.toMap(RoleCount::getRole, RoleCount::getCount))
        );
    }

    public void deactivate(UUID targetUserId, UUID callerUserId) {
        requireNotSelf(targetUserId, callerUserId);

        User user = findStaffOrThrow(targetUserId);
        user.setActive(false);
        userRepository.save(user);
    }

    public void reactivate(UUID targetUserId) {
        User user = findStaffOrThrow(targetUserId);
        user.setActive(true);
        userRepository.save(user);
    }

    public UserResponseDTO changeRole(UUID targetUserId, UUID callerUserId, ChangeRoleRequestDTO request) {
        requireNotSelf(targetUserId, callerUserId);
        requireAssignableRole(request.role());

        User user = findStaffOrThrow(targetUserId);
        user.setRole(request.role());

        return UserResponseDTO.from(userRepository.save(user));
    }

    public void resetPassword(UUID targetUserId, AdminPasswordResetRequestDTO request) {
        User user = findStaffOrThrow(targetUserId);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public void changeOwnPassword(UUID callerUserId, ChangeOwnPasswordRequestDTO request) {
        User user = userRepository.findById(callerUserId)
                .orElseThrow(() -> new ResourceNotFoundException(SecurityMessages.USER_NOT_FOUND.formatted(callerUserId)));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException(SecurityMessages.CURRENT_PASSWORD_INCORRECT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User findStaffOrThrow(UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(SecurityMessages.USER_NOT_FOUND.formatted(userId)));
    }

    private void requireAssignableRole(Role role) {
        if (!ASSIGNABLE_ROLES.contains(role)) {
            throw new ForbiddenOperationException(SecurityMessages.ROLE_NOT_ASSIGNABLE.formatted(role));
        }
    }

    private void requireNotSelf(UUID targetUserId, UUID callerUserId) {
        if (targetUserId.equals(callerUserId)) {
            throw new InvalidRequestException(SecurityMessages.CANNOT_ACT_ON_OWN_ACCOUNT);
        }
    }
}
