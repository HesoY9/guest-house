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

    List<Reservation> findByGuestIdOrderByCheckInDateDesc(Long guestId);

    List<Reservation> findByStatus(ReservationStatus status);

    boolean existsByRoomId(Long roomId);

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

    // Every non-cancelled stay touching [from, to) - for the room calendar and occupancy reports.
    @Query("SELECT r FROM Reservation r " +
           "WHERE r.status <> com.hesoy9.guesthouse.entity.ReservationStatus.CANCELLED " +
           "AND r.checkInDate < :to AND r.checkOutDate > :from " +
           "ORDER BY r.checkInDate")
    List<Reservation> findStaysOverlapping(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Staff list: optional status filter + optional free-text search on guest name or reference number.
    @Query("SELECT r FROM Reservation r " +
           "WHERE (:status IS NULL OR r.status = :status) " +
           "AND (:q IS NULL OR LOWER(r.guest.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR CAST(r.id AS string) = :q OR r.room.roomNumber = :q) " +
           "ORDER BY r.checkInDate DESC, r.id DESC")
    List<Reservation> search(@Param("status") ReservationStatus status, @Param("q") String q);

    // Guests who are due to arrive (today or overdue) and haven't been checked in yet.
    @Query("SELECT r FROM Reservation r " +
           "WHERE r.status = com.hesoy9.guesthouse.entity.ReservationStatus.ACTIVE " +
           "AND r.checkedInAt IS NULL AND r.checkInDate <= :today AND r.checkOutDate > :today " +
           "ORDER BY r.checkInDate, r.room.roomNumber")
    List<Reservation> findArrivals(@Param("today") LocalDate today);

    // Checked-in guests whose stay ends today (or should have ended already).
    @Query("SELECT r FROM Reservation r " +
           "WHERE r.status = com.hesoy9.guesthouse.entity.ReservationStatus.ACTIVE " +
           "AND r.checkedInAt IS NOT NULL AND r.checkOutDate <= :today " +
           "ORDER BY r.checkOutDate, r.room.roomNumber")
    List<Reservation> findDepartures(@Param("today") LocalDate today);

    @Query("SELECT r FROM Reservation r " +
           "WHERE r.status = com.hesoy9.guesthouse.entity.ReservationStatus.ACTIVE " +
           "AND r.checkedInAt IS NOT NULL ORDER BY r.room.roomNumber")
    List<Reservation> findInHouse();

    // Next not-yet-arrived booking per room, for the housekeeping board ("guest arriving today").
    @Query("SELECT r FROM Reservation r " +
           "WHERE r.status = com.hesoy9.guesthouse.entity.ReservationStatus.ACTIVE " +
           "AND r.checkedInAt IS NULL AND r.checkOutDate > :today " +
           "ORDER BY r.checkInDate")
    List<Reservation> findUpcomingNotArrived(@Param("today") LocalDate today);

    // How many stays each guest has had (cancelled ones don't count) - for the guest list.
    @Query("SELECT r.guest.id, COUNT(r) FROM Reservation r " +
           "WHERE r.status <> com.hesoy9.guesthouse.entity.ReservationStatus.CANCELLED " +
           "GROUP BY r.guest.id")
    List<Object[]> countStaysByGuest();

    List<Reservation> findByCheckInDateBetweenOrderByCheckInDate(LocalDate from, LocalDate to);
}
