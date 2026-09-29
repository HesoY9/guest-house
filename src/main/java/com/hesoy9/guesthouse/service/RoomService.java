package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.entity.RoomStatus;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room addRoom(Room room) { // FR1
        return roomRepository.save(room);
    }

    public Room updateRoom(Long roomId, Room updatedDetails) { // FR2
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        room.setRoomNumber(updatedDetails.getRoomNumber());
        room.setType(updatedDetails.getType());
        room.setPrice(updatedDetails.getPrice());
        room.setStatus(updatedDetails.getStatus());
        room.setDescription(updatedDetails.getDescription());
        room.setImageUrl(updatedDetails.getImageUrl());
        return roomRepository.save(room);
    }

    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

    public void removeRoom(Long roomId) { // FR3
        roomRepository.deleteById(roomId);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getAvailableRooms() { // UR5
        return roomRepository.findByStatus(RoomStatus.AVAILABLE);
    }

    // Public site search - date-aware, unlike getAvailableRooms() above.
    public List<Room> getAvailableRoomsForDates(LocalDate checkIn, LocalDate checkOut) {
        return roomRepository.findAvailableRoomsForDateRange(checkIn, checkOut);
    }

    // Shared by check-in/out (FR15, FR18) and housekeeping (FR24, FR25)
    public Room updateStatus(Long roomId, RoomStatus status) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        room.setStatus(status);
        return roomRepository.save(room);
    }
}
