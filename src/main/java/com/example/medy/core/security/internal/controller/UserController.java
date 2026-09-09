package com.example.medy.core.security.internal.controller;

import com.example.medy.core.security.internal.dto.RegisterStaffRequestDTO;
import com.example.medy.core.security.internal.dto.UserResponseDTO;
import com.example.medy.core.security.internal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
