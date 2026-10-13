package com.hesoy9.guesthouse.entity;

// Where a reservation is in its life, worked out from its status, dates and check-in time.
// Not stored - see Reservation.getStage(). Drives badges, dashboard lists and the room calendar.
public enum ReservationStage {
    UPCOMING("Upcoming", "info"),
    ARRIVING("Arriving", "warning"),
    IN_HOUSE("In house", "success"),
    DUE_OUT("Due out", "warning"),
    NO_SHOW("No-show", "danger"),
    COMPLETED("Completed", "muted"),
    CANCELLED("Cancelled", "muted");

    private final String label;
    private final String tone; // CSS badge colour

    ReservationStage(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() {
        return label;
    }

    public String getTone() {
        return tone;
    }
}
