package com.troshchii.booking.event;

import com.troshchii.booking.common.EnumConverter;
import jakarta.persistence.Converter;

@Converter
public class EventSeatStatusConverter extends EnumConverter<EventSeatStatus> {
    public EventSeatStatusConverter() {
        super(EventSeatStatus.class);
    }
}
