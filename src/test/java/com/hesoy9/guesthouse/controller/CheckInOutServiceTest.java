package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.*;
import com.hesoy9.guesthouse.repository.GuestRepository;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInOutServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BillingService billingService;

    @Mock
    private GuestRepository guestRepository;

    @InjectMocks
    private CheckInOutService checkInOutService;

    private final LocalDate today = LocalDate.now();

    @Test
    void checkIn_throwsWhenGuestHasNoId() {
        Guest guest = new Guest();
        guest.setIdOrPassport(""); // blank - not allowed (DR3)

        Reservation reservation = arrivingToday(guest, roomWithStatus(RoomStatus.RESERVED));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> checkInOutService.checkIn(1L));
    }

    @Test
    void checkIn_marksRoomOccupiedWhenGuestHasId() {
        Guest guest = new Guest();
        guest.setIdOrPassport("P99999");
        Room room = roomWithStatus(RoomStatus.RESERVED);

        Reservation reservation = arrivingToday(guest, room);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        checkInOutService.checkIn(1L);

        assertEquals(RoomStatus.OCCUPIED, room.getStatus()); // FR15
        assertTrue(reservation.isCheckedIn());
        verify(roomRepository).save(room);
    }

    @Test
    void checkIn_recordsIdGivenAtTheDesk() {
        Guest guest = new Guest(); // booked online, no ID yet
        Room room = roomWithStatus(RoomStatus.AVAILABLE);

        Reservation reservation = arrivingToday(guest, room);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(guestRepository.findByIdOrPassport("N1234567")).thenReturn(Optional.empty());

        checkInOutService.checkIn(1L, "N1234567");

        assertEquals("N1234567", guest.getIdOrPassport());
        assertEquals(RoomStatus.OCCUPIED, room.getStatus());
    }

    @Test
    void checkIn_refusedWhileRoomNeedsCleaning() {
        Guest guest = new Guest();
        guest.setIdOrPassport("P1");
        Reservation reservation = arrivingToday(guest, roomWithStatus(RoomStatus.CLEANING_REQUIRED));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> checkInOutService.checkIn(1L)); // DR5
        assertFalse(reservation.isCheckedIn());
    }

    @Test
    void checkIn_refusedBeforeArrivalDate() {
        Guest guest = new Guest();
        guest.setIdOrPassport("P1");
        Reservation reservation = arrivingToday(guest, roomWithStatus(RoomStatus.AVAILABLE));
        reservation.setCheckInDate(today.plusDays(3));
        reservation.setCheckOutDate(today.plusDays(5));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> checkInOutService.checkIn(1L));
    }

    @Test
    void checkOut_marksRoomCleaningRequiredAndGeneratesInvoice() {
        Room room = roomWithStatus(RoomStatus.OCCUPIED);

        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setCheckedInAt(today.minusDays(2).atTime(14, 0));

        Invoice expectedInvoice = new Invoice();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(billingService.getOrCreateInvoice(reservation)).thenReturn(expectedInvoice);

        Invoice result = checkInOutService.checkOut(1L);

        assertEquals(ReservationStatus.COMPLETED, reservation.getStatus());
        assertEquals(RoomStatus.CLEANING_REQUIRED, room.getStatus()); // FR18
        assertNotNull(reservation.getCheckedOutAt());
        assertSame(expectedInvoice, result);
    }

    @Test
    void checkOut_refusedWhenNeverCheckedIn() {
        Reservation reservation = new Reservation();
        reservation.setRoom(roomWithStatus(RoomStatus.RESERVED));
        reservation.setStatus(ReservationStatus.ACTIVE);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> checkInOutService.checkOut(1L));
        verify(billingService, never()).getOrCreateInvoice(any());
    }

    private Reservation arrivingToday(Guest guest, Room room) {
        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoom(room);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setCheckInDate(today);
        reservation.setCheckOutDate(today.plusDays(2));
        return reservation;
    }

    private static Room roomWithStatus(RoomStatus status) {
        Room room = new Room();
        room.setRoomNumber("101");
        room.setStatus(status);
        return room;
    }
}
