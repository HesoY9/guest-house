package com.hesoy9.guesthouse.entity;

public enum BookingChannel {
    ONLINE_PORTAL("Online"),
    IN_PERSON("Walk-in"),
    PHONE("Phone");

    private final String label;

    BookingChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
