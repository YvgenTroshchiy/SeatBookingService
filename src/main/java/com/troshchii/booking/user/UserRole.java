package com.troshchii.booking.user;

import com.fasterxml.jackson.annotation.JsonValue;
import com.troshchii.booking.common.PersistableEnum;

public enum UserRole implements PersistableEnum {
    USER("user"),
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }
}
