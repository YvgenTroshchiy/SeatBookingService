package com.troshchii.booking.event;

import com.fasterxml.jackson.annotation.JsonValue;
import com.troshchii.booking.common.PersistableEnum;

public enum EventType implements PersistableEnum {
    CONCERT("concert"),
    CINEMA("cinema"),
    FLIGHT("flight");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }
}
