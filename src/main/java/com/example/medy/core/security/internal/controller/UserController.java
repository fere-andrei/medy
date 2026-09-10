package com.example.medy.core.security.internal.controller;

import com.example.medy.core.security.internal.dto.AdminPasswordResetRequestDTO;
import com.example.medy.core.security.internal.dto.ChangeOwnPasswordRequestDTO;
import com.example.medy.core.security.internal.dto.ChangeRoleRequestDTO;
import com.example.medy.core.security.internal.dto.RegisterStaffRequestDTO;
import com.example.medy.core.security.internal.dto.UserResponseDTO;
import com.example.medy.core.security.internal.jwt.JwtPrincipal;
import com.example.medy.core.security.internal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponseDTO registerStaff(@Valid @RequestBody RegisterStaffRequestDTO request) {
        return userService.registerStaff(request);
    }

    @GetMapping
    List<UserResponseDTO> list() {
        return userService.list();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable UUID id, @AuthenticationPrincipal JwtPrincipal caller) {
        userService.deactivate(id, caller.userId());
    }

    @PutMapping("/{id}/role")
    UserResponseDTO changeRole(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal caller,
            @Valid @RequestBody ChangeRoleRequestDTO request) {
        return userService.changeRole(id, caller.userId(), request);
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@PathVariable UUID id, @Valid @RequestBody AdminPasswordResetRequestDTO request) {
        userService.resetPassword(id, request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeOwnPassword(
            @AuthenticationPrincipal JwtPrincipal caller, @Valid @RequestBody ChangeOwnPasswordRequestDTO request) {
        userService.changeOwnPassword(caller.userId(), request);
    }
}
