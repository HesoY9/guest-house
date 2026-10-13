package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.dto.RoomForm;
import com.hesoy9.guesthouse.dto.RoomSearch;
import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.entity.RoomPhoto;
import com.hesoy9.guesthouse.entity.RoomStatus;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final PhotoStorageService photoStorageService;

    public RoomService(RoomRepository roomRepository,
                       ReservationRepository reservationRepository,
                       PhotoStorageService photoStorageService) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.photoStorageService = photoStorageService;
    }

    public Room addRoom(Room room) { // FR1
        ensureRoomNumberFree(room.getRoomNumber(), null);
        if (room.getStatus() == null) {
            room.setStatus(RoomStatus.AVAILABLE);
        }
        return roomRepository.save(room);
    }

    public Room updateRoom(Long roomId, Room updatedDetails) { // FR2
        Room room = getRoomById(roomId);
        ensureRoomNumberFree(updatedDetails.getRoomNumber(), roomId);
        room.setRoomNumber(updatedDetails.getRoomNumber());
        room.setType(updatedDetails.getType());
        room.setPrice(updatedDetails.getPrice());
        room.setStatus(updatedDetails.getStatus());
        room.setCapacity(updatedDetails.getCapacity());
        room.setDescription(updatedDetails.getDescription());
        room.setAmenities(updatedDetails.getAmenities());
        return roomRepository.save(room);
    }

    // Staff "Add room" panel
    public Room createRoom(RoomForm form) {
        ensureRoomNumberFree(form.getRoomNumber(), null);
        Room room = new Room();
        form.applyTo(room);
        return roomRepository.save(room);
    }

    // Staff "Edit room" panel - the uniqueness check runs before anything is changed.
    @Transactional
    public Room updateRoom(Long roomId, RoomForm form) {
        ensureRoomNumberFree(form.getRoomNumber(), roomId);
        Room room = getRoomById(roomId);
        form.applyTo(room);
        return roomRepository.save(room);
    }

    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

    @Transactional
    public void removeRoom(Long roomId) { // FR3
        Room room = getRoomById(roomId);
        // Deleting a room would orphan its booking history and invoices.
        if (reservationRepository.existsByRoomId(roomId)) {
            throw new IllegalStateException("Room " + room.getRoomNumber()
                    + " has reservations on record, so it can't be deleted");
        }
        room.getPhotos().forEach(p -> photoStorageService.deleteIfStored(p.getUrl()));
        roomRepository.delete(room);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAllByOrderByRoomNumberAsc();
    }

    public List<Room> getAvailableRooms() { // UR5
        return roomRepository.findByStatus(RoomStatus.AVAILABLE);
    }

    // Public site search - date-aware, unlike getAvailableRooms() above.
    public List<Room> getAvailableRoomsForDates(LocalDate checkIn, LocalDate checkOut) {
        return roomRepository.findAvailableRoomsForDateRange(checkIn, checkOut);
    }

    // Public search with filters and sorting. A guest house has tens of rooms, not thousands,
    // so filtering in memory keeps this simple and still answers instantly (NFR1).
    public List<Room> search(RoomSearch criteria) {
        List<Room> rooms = criteria.hasDates()
                ? getAvailableRoomsForDates(criteria.getCheckIn(), criteria.getCheckOut())
                : getAllRooms();

        Stream<Room> stream = rooms.stream();
        if (criteria.getGuests() != null) {
            stream = stream.filter(r -> r.getCapacity() == null || r.getCapacity() >= criteria.getGuests());
        }
        if (!criteria.getType().isEmpty()) {
            stream = stream.filter(r -> criteria.getType().stream().anyMatch(t -> t.equalsIgnoreCase(r.getType())));
        }
        if (criteria.getMaxPrice() != null) {
            stream = stream.filter(r -> r.getPrice() <= criteria.getMaxPrice());
        }
        if (!criteria.getAmenity().isEmpty()) {
            stream = stream.filter(r -> r.getAmenities().containsAll(criteria.getAmenity()));
        }

        Comparator<Room> order = switch (Objects.requireNonNullElse(criteria.getSort(), "")) {
            case "price-asc" -> Comparator.comparing(Room::getPrice);
            case "price-desc" -> Comparator.comparing(Room::getPrice).reversed();
            case "capacity" -> Comparator.comparing((Room r) -> r.getCapacity() == null ? 0 : r.getCapacity()).reversed()
                    .thenComparing(Room::getPrice);
            // "recommended": rooms with photos first (they convert better), then cheapest
            default -> Comparator.comparing((Room r) -> r.getPhotos().isEmpty()).thenComparing(Room::getPrice);
        };
        return stream.sorted(order).toList();
    }

    // Distinct room types for the search filter checkboxes.
    public List<String> getRoomTypes() {
        return getAllRooms().stream().map(Room::getType).distinct().sorted().toList();
    }

    // Shared by check-in/out (FR15, FR18) and housekeeping (FR24, FR25)
    public Room updateStatus(Long roomId, RoomStatus status) {
        Room room = getRoomById(roomId);
        room.setStatus(status);
        return roomRepository.save(room);
    }

    // Housekeeping board one-click action (FR25): a cleaned room becomes available again (DR5).
    public Room markClean(Long roomId) {
        Room room = getRoomById(roomId);
        if (room.getStatus() != RoomStatus.CLEANING_REQUIRED) {
            throw new IllegalStateException("Room " + room.getRoomNumber() + " isn't waiting for cleaning");
        }
        room.setStatus(RoomStatus.AVAILABLE);
        return roomRepository.save(room);
    }

    // Housekeeping can flag a vacant room that needs attention (FR24).
    public Room markNeedsCleaning(Long roomId) {
        Room room = getRoomById(roomId);
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new IllegalStateException("Room " + room.getRoomNumber() + " is occupied - it will be flagged automatically at check-out");
        }
        room.setStatus(RoomStatus.CLEANING_REQUIRED);
        return roomRepository.save(room);
    }

    // ---- photos ----

    @Transactional
    public Room addPhotoUrl(Long roomId, String url) {
        Room room = getRoomById(roomId);
        room.addPhoto(url.trim());
        return roomRepository.save(room);
    }

    @Transactional
    public void removePhoto(Long roomId, Long photoId) {
        Room room = getRoomById(roomId);
        RoomPhoto photo = findPhoto(room, photoId);
        room.getPhotos().remove(photo);
        roomRepository.save(room);
        photoStorageService.deleteIfStored(photo.getUrl());
    }

    // Moves a photo to the front, making it the cover image on cards.
    @Transactional
    public void makeCover(Long roomId, Long photoId) {
        Room room = getRoomById(roomId);
        RoomPhoto cover = findPhoto(room, photoId);
        int order = 1;
        for (RoomPhoto photo : room.getPhotos()) {
            photo.setSortOrder(photo == cover ? 0 : order++);
        }
        roomRepository.save(room);
    }

    private RoomPhoto findPhoto(Room room, Long photoId) {
        return room.getPhotos().stream()
                .filter(p -> p.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));
    }

    private void ensureRoomNumberFree(String roomNumber, Long ignoreRoomId) {
        if (roomNumber == null) return;
        roomRepository.findByRoomNumber(roomNumber.trim())
                .filter(existing -> !existing.getId().equals(ignoreRoomId))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Room number " + roomNumber.trim() + " already exists");
                });
    }
}
