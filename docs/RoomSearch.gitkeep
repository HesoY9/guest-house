package com.hesoy9.guesthouse.dto;

import com.hesoy9.guesthouse.entity.Amenity;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// Query-string filters for the public room search (/rooms?checkIn=...&guests=2&type=Double&sort=price).
public class RoomSearch {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    private Integer guests;
    private List<String> type = new ArrayList<>();
    private Double maxPrice;
    private Set<Amenity> amenity = EnumSet.noneOf(Amenity.class);
    private String sort = "recommended"; // recommended | price-asc | price-desc | capacity

    public boolean hasDates() {
        return checkIn != null && checkOut != null && checkOut.isAfter(checkIn);
    }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public Integer getGuests() { return guests; }
    public void setGuests(Integer guests) { this.guests = guests; }

    public List<String> getType() { return type; }
    public void setType(List<String> type) { this.type = type != null ? type : new ArrayList<>(); }

    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }

    public Set<Amenity> getAmenity() { return amenity; }
    public void setAmenity(Set<Amenity> amenity) { this.amenity = amenity != null ? amenity : EnumSet.noneOf(Amenity.class); }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
