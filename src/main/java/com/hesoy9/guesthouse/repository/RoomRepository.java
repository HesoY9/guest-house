package com.hesoy9.guesthouse.repository;

import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // SELECT * FROM rooms WHERE status = ?
    List<Room> findByStatus(RoomStatus status);

    // SELECT * FROM rooms WHERE room_number = ?
    Optional<Room> findByRoomNumber(String roomNumber);

    // Same overlap logic as ReservationRepository.findOverlappingReservations, just inverted:
    // a room qualifies unless it has an ACTIVE reservation overlapping the requested dates.
    // This is date-aware, unlike the "status" field, which is only a current snapshot.
    @Query("SELECT r FROM Room r WHERE r.id NOT IN (" +
           "SELECT res.room.id FROM Reservation res " +
           "WHERE res.status = com.hesoy9.guesthouse.entity.ReservationStatus.ACTIVE " +
           "AND res.checkInDate < :checkOutDate AND res.checkOutDate > :checkInDate)")
    List<Room> findAvailableRoomsForDateRange(
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate);
}
