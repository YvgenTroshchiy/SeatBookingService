package com.troshchii.booking.common;

import jakarta.persistence.AttributeConverter;

import java.util.Arrays;

public abstract class EnumConverter<E extends Enum<E> & PersistableEnum> implements AttributeConverter<E, String> {

    private final Class<E> enumClass;

    protected EnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getValue().equals(dbData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown enum value: " + dbData));
    }
}
