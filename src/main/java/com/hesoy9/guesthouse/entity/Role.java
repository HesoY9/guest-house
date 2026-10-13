package com.hesoy9.guesthouse.entity;

public enum Role {
    ADMIN("Administrator"),
    RECEPTIONIST("Receptionist"),
    HOUSEKEEPING("Housekeeping"),
    MANAGER("Manager");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
