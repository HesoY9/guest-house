package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.*;
import com.hesoy9.guesthouse.repository.GuestRepository;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private GuestRepository guestRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Guest guest;
    private Room room;

    @BeforeEach
    void setUp() {
        guest = new Guest();
        guest.setIdOrPassport("P12345");
        guest.setName("Test Guest");

        room = new Room();
        room.setRoomNumber("101");
        room.setPrice(50.0);
        room.setStatus(RoomStatus.AVAILABLE);
    }

    @Test
    void createReservation_throwsWhenCheckOutNotAfterCheckIn() {
        LocalDate checkIn = LocalDate.of(2026, 1, 10);
        LocalDate checkOut = LocalDate.of(2026, 1, 10); // same day - invalid (DR2)

        assertThrows(IllegalArgumentException.class, () ->
                reservationService.createReservation(1L, 1L, checkIn, checkOut, 2, BookingChannel.ONLINE_PORTAL));

        // the date check should fail before any repository is even touched
        verifyNoInteractions(reservationRepository, roomRepository, guestRepository);
    }

    @Test
    void createReservation_throwsWhenRoomAlreadyBooked() {
        LocalDate checkIn = LocalDate.of(2026, 1, 10);
        LocalDate checkOut = LocalDate.of(2026, 1, 12);

        Reservation existing = new Reservation();
        when(reservationRepository.findOverlappingReservations(1L, checkIn, checkOut))
                .thenReturn(List.of(existing)); // simulate an overlap (DR1)

        assertThrows(IllegalStateException.class, () ->
                reservationService.createReservation(1L, 1L, checkIn, checkOut, 2, BookingChannel.ONLINE_PORTAL));
    }

    @Test
    void createReservation_savesAndReservesRoomWhenAvailable() {
        LocalDate checkIn = LocalDate.of(2026, 1, 10);
        LocalDate checkOut = LocalDate.of(2026, 1, 12);

        when(reservationRepository.findOverlappingReservations(1L, checkIn, checkOut))
                .thenReturn(Collections.emptyList()); // no overlap
        when(guestRepository.findById(1L)).thenReturn(Optional.of(guest));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = reservationService.createReservation(
                1L, 1L, checkIn, checkOut, 2, BookingChannel.ONLINE_PORTAL);

        assertEquals(ReservationStatus.ACTIVE, result.getStatus());
        assertEquals(RoomStatus.RESERVED, room.getStatus()); // room object was updated in place
        verify(roomRepository).save(room);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void cancelReservation_setsRoomBackToAvailable() {
        room.setStatus(RoomStatus.RESERVED);

        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setRoom(room);

        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        reservationService.cancelReservation(5L);

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
    }
}
