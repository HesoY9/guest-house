package com.hesoy9.guesthouse.entity;

public enum RoomStatus {
    AVAILABLE("Ready"),
    RESERVED("Reserved"),
    OCCUPIED("Occupied"),
    CLEANING_REQUIRED("Needs cleaning");

    private final String label;

    RoomStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
