package com.example.medy.core.security.internal.repository.projection;
import com.example.medy.core.security.internal.enums.Role;


public interface RoleCount {
    Role getRole();
    Long getCount();
}
