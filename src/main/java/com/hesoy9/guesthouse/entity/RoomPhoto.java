package com.hesoy9.guesthouse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "room_photos")
public class RoomPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @JsonIgnore: Room -> photos -> room -> photos ... would loop forever in the REST API
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // Either an uploaded file ("/uploads/rooms/3/abc.jpg"), a full URL, or a legacy
    // filename under /static/images/. Formats.photo() turns any of these into a usable src.
    @Column(nullable = false, length = 500)
    private String url;

    // Lowest number is the cover photo shown on cards.
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public RoomPhoto() {
        // required by JPA
    }

    public RoomPhoto(Room room, String url, Integer sortOrder) {
        this.room = room;
        this.url = url;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // A value usable directly as <img src>: full URLs and uploaded files ("/uploads/...") as-is,
    // bare filenames (the old way) from /static/images/.
    public String getSrc() {
        return toSrc(url);
    }

    public static String toSrc(String url) {
        if (url == null || url.isBlank()) return null;
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/")) return url;
        return "/images/" + url;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
