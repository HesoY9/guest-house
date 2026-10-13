package com.hesoy9.guesthouse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Room number is required")
    @Column(name = "room_number", nullable = false, unique = true, length = 10)
    private String roomNumber;

    @NotBlank(message = "Room type is required")
    @Column(nullable = false, length = 30)
    private String type; // e.g. "Single", "Double", "Suite"

    // Price per night in LKR
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    @Column(nullable = false)
    private Double price;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    // Maximum number of guests. Nullable in the database only so 'ddl-auto=update' can add the
    // column to an existing table - DataSeeder back-fills old rows, and new rooms must set it.
    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Column
    private Integer capacity;

    // Marketing content for the public guest site - optional, no validation, edit anytime.
    @Column(length = 1000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_amenities", joinColumns = @JoinColumn(name = "room_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "amenity", length = 30, nullable = false)
    private Set<Amenity> amenities = new HashSet<>();

    // Several photos per room; the first one (lowest sortOrder) is the cover.
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<RoomPhoto> photos = new ArrayList<>();

    // Legacy single-image column from before photos existed. DataSeeder copies any value
    // here into a RoomPhoto on startup and clears it - nothing else should use it.
    @JsonIgnore
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    public Room() {
        // required by JPA
    }

    public Long getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Amenity> getAmenities() {
        return amenities;
    }

    public void setAmenities(Set<Amenity> amenities) {
        this.amenities.clear();
        if (amenities != null) {
            this.amenities.addAll(amenities);
        }
    }

    // Always in the enum's declared order, so every page lists amenities the same way.
    @JsonIgnore
    public Set<Amenity> getSortedAmenities() {
        return new TreeSet<>(amenities);
    }

    public List<RoomPhoto> getPhotos() {
        return photos;
    }

    public void addPhoto(String url) {
        int next = photos.stream().mapToInt(RoomPhoto::getSortOrder).max().orElse(-1) + 1;
        photos.add(new RoomPhoto(this, url, next));
    }

    // The cover photo for cards and summaries, or null when the room has no photos yet.
    @JsonIgnore
    public String getCoverPhotoUrl() {
        return photos.isEmpty() ? null : photos.get(0).getUrl();
    }

    // Cover photo as an <img src> value (see RoomPhoto.getSrc), or null.
    @JsonIgnore
    public String getCoverPhotoSrc() {
        return photos.isEmpty() ? null : photos.get(0).getSrc();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
