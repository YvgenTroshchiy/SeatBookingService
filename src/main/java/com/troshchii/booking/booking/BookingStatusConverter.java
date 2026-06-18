package com.troshchii.booking.booking;

import com.troshchii.booking.common.EnumConverter;
import jakarta.persistence.Converter;

@Converter
public class BookingStatusConverter extends EnumConverter<BookingStatus> {
    public BookingStatusConverter() {
        super(BookingStatus.class);
    }
}
