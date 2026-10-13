package com.hesoy9.guesthouse.entity;

// Fixed list so the public site can filter on it and staff pick from checkboxes
// instead of typing free text that would never match up.
public enum Amenity {
    WIFI("Free Wi-Fi"),
    AIR_CONDITIONING("Air conditioning"),
    CEILING_FAN("Ceiling fan"),
    HOT_WATER("Hot water"),
    PRIVATE_BATHROOM("Private bathroom"),
    SEA_VIEW("Sea view"),
    BALCONY("Balcony"),
    BEACH_ACCESS("Beach access"),
    TV("TV"),
    MINI_FRIDGE("Mini fridge"),
    KITCHENETTE("Kitchenette"),
    WORK_DESK("Work desk"),
    BREAKFAST("Breakfast included"),
    PARKING("Free parking");

    private final String label;

    Amenity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
