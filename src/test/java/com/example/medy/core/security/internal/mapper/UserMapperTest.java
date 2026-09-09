package com.example.medy.core.security.internal.mapper;

import com.example.medy.core.security.internal.dto.RegisterStaffRequestDTO;
import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Test
    void toEntity_mapsEmailFullNameAndRole_andLeavesTenantScopedFieldsUnset() {
        RegisterStaffRequestDTO request =
                new RegisterStaffRequestDTO("doctor@test.com", "securePass1", "Dr. Test", Role.DOCTOR);

        User user = mapper.toEntity(request);

        assertThat(user.getId()).isNull();
        assertThat(user.getTenantId()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getEmail()).isEqualTo("doctor@test.com");
        assertThat(user.getFullName()).isEqualTo("Dr. Test");
        assertThat(user.getRole()).isEqualTo(Role.DOCTOR);
    }
}
