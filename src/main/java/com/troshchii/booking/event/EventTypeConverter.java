package com.troshchii.booking.event;

import com.troshchii.booking.common.EnumConverter;
import jakarta.persistence.Converter;

@Converter
public class EventTypeConverter extends EnumConverter<EventType> {
    public EventTypeConverter() {
        super(EventType.class);
    }
}
