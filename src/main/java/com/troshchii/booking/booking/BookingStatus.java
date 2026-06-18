package com.troshchii.booking.booking;

import com.fasterxml.jackson.annotation.JsonValue;
import com.troshchii.booking.common.PersistableEnum;

public enum BookingStatus implements PersistableEnum {
    CONFIRMED("confirmed"),
    CANCELLED("cancelled");

    private final String value;

    BookingStatus(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }
}
