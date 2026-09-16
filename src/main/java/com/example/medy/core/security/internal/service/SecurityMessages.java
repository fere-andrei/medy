package com.example.medy.core.security.internal.service;

/** Every user-facing error message thrown by {@link AuthService} and {@link UserService}, in one place. */
final class SecurityMessages {

    static final String INVALID_CREDENTIALS = "Invalid credentials";
    static final String EMAIL_ALREADY_REGISTERED = "Email is already registered in this clinic";
    static final String CURRENT_PASSWORD_INCORRECT = "Current password is incorrect";
    static final String CANNOT_ACT_ON_OWN_ACCOUNT = "You cannot do this action on your own account";
    static final String USER_NOT_FOUND = "User %s not found";
    static final String ROLE_NOT_ASSIGNABLE = "Role %s cannot be assigned through this endpoint";

    private SecurityMessages() {
    }
}
