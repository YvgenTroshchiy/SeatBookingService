package com.troshchii.booking.event;

import com.fasterxml.jackson.annotation.JsonValue;
import com.troshchii.booking.common.PersistableEnum;

public enum EventSeatStatus implements PersistableEnum {
    AVAILABLE("available"),
    HELD("held"),
    BOOKED("booked");

    private final String value;

    EventSeatStatus(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }
}
