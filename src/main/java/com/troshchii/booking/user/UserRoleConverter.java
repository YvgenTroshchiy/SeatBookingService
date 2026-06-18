package com.troshchii.booking.user;

import com.troshchii.booking.common.EnumConverter;
import jakarta.persistence.Converter;

@Converter
public class UserRoleConverter extends EnumConverter<UserRole> {
    public UserRoleConverter() {
        super(UserRole.class);
    }
}
