package com.hesoy9.guesthouse.repository;

import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByGuestId(Long guestId);

    List<Reservation> findByStatus(ReservationStatus status);

    // Two date ranges overlap when: existing.checkIn < new.checkOut AND existing.checkOut > new.checkIn.
    // Only ACTIVE reservations count - a cancelled one shouldn't block the room.
    // Call this before saving a new reservation; a non-empty result means the room is already booked (DR1).
    @Query("SELECT r FROM Reservation r " +
           "WHERE r.room.id = :roomId " +
           "AND r.status = com.hesoy9.guesthouse.entity.ReservationStatus.ACTIVE " +
           "AND r.checkInDate < :checkOutDate " +
           "AND r.checkOutDate > :checkInDate")
    List<Reservation> findOverlappingReservations(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate);
}
