package com.hesoy9.guesthouse.dto;

import com.hesoy9.guesthouse.entity.Amenity;
import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.entity.RoomStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.EnumSet;
import java.util.Set;

// What the staff "Add / Edit room" panel submits. A separate class (not the Room entity itself)
// so a form post can never touch fields it shouldn't, like the id or the photo list.
public class RoomForm {

    @NotBlank(message = "Room number is required")
    @Size(max = 10, message = "Room number can be at most 10 characters")
    private String roomNumber;

    @NotBlank(message = "Room type is required")
    @Size(max = 30, message = "Room type can be at most 30 characters")
    private String type;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private Double price;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 20, message = "Capacity can be at most 20")
    private Integer capacity;

    @NotNull(message = "Status is required")
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Size(max = 1000, message = "Description can be at most 1000 characters")
    private String description;

    private Set<Amenity> amenities = EnumSet.noneOf(Amenity.class);

    public static RoomForm from(Room room) {
        RoomForm form = new RoomForm();
        form.roomNumber = room.getRoomNumber();
        form.type = room.getType();
        form.price = room.getPrice();
        form.capacity = room.getCapacity();
        form.status = room.getStatus();
        form.description = room.getDescription();
        form.amenities = room.getAmenities().isEmpty() ? EnumSet.noneOf(Amenity.class) : EnumSet.copyOf(room.getAmenities());
        return form;
    }

    public void applyTo(Room room) {
        room.setRoomNumber(roomNumber.trim());
        room.setType(type.trim());
        room.setPrice(price);
        room.setCapacity(capacity);
        room.setStatus(status);
        room.setDescription(description == null || description.isBlank() ? null : description.trim());
        room.setAmenities(amenities);
    }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<Amenity> getAmenities() { return amenities; }
    public void setAmenities(Set<Amenity> amenities) { this.amenities = amenities != null ? amenities : EnumSet.noneOf(Amenity.class); }
}
