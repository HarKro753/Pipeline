package com.pipeline.domain.valueobject;

import java.util.Objects;

public final class Barcode {

    private final String value;

    public Barcode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Barcode must not be empty");
        }
        if (!value.matches("\\d{8,14}")) {
            throw new IllegalArgumentException("Barcode must be 8-14 digits: " + value);
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Barcode other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
