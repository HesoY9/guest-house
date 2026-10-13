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

import java.lang.reflect.Field;
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

    // Dates relative to today, so the "no past dates" rule never makes these tests go stale.
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() throws Exception {
        guest = new Guest();
        guest.setIdOrPassport("P12345");
        guest.setName("Test Guest");
        setId(guest, 7L);

        room = new Room();
        room.setRoomNumber("101");
        room.setPrice(50.0);
        room.setCapacity(2);
        room.setStatus(RoomStatus.AVAILABLE);
    }

    @Test
    void createReservation_throwsWhenCheckOutNotAfterCheckIn() {
        LocalDate checkIn = today.plusDays(10);
        LocalDate checkOut = today.plusDays(10); // same day - invalid (DR2)

        assertThrows(IllegalArgumentException.class, () ->
                reservationService.createReservation(1L, 1L, checkIn, checkOut, 2, BookingChannel.ONLINE_PORTAL));

        // the date check should fail before any repository is even touched
        verifyNoInteractions(reservationRepository, roomRepository, guestRepository);
    }

    @Test
    void createReservation_throwsWhenCheckInIsInThePast() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservationService.createReservation(1L, 1L, today.minusDays(1), today.plusDays(2), 2, BookingChannel.IN_PERSON));

        assertTrue(ex.getMessage().contains("past"));
        verifyNoInteractions(reservationRepository, roomRepository, guestRepository);
    }

    @Test
    void createReservation_throwsWhenGuestsExceedCapacity() {
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room)); // sleeps 2

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservationService.createReservation(1L, 1L, today.plusDays(3), today.plusDays(5), 3, BookingChannel.ONLINE_PORTAL));

        assertTrue(ex.getMessage().contains("sleeps at most 2"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_throwsWhenNoGuests() {
        assertThrows(IllegalArgumentException.class, () ->
                reservationService.createReservation(1L, 1L, today.plusDays(3), today.plusDays(5), 0, BookingChannel.ONLINE_PORTAL));
    }

    @Test
    void createReservation_throwsWhenRoomAlreadyBooked() {
        LocalDate checkIn = today.plusDays(10);
        LocalDate checkOut = today.plusDays(12);

        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.findOverlappingReservations(1L, checkIn, checkOut))
                .thenReturn(List.of(new Reservation())); // simulate an overlap (DR1)

        assertThrows(IllegalStateException.class, () ->
                reservationService.createReservation(1L, 1L, checkIn, checkOut, 2, BookingChannel.ONLINE_PORTAL));
    }

    @Test
    void createReservation_sameDayArrivalReservesRoom() {
        LocalDate checkIn = today;
        LocalDate checkOut = today.plusDays(2);

        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.findOverlappingReservations(1L, checkIn, checkOut))
                .thenReturn(Collections.emptyList()); // no overlap
        when(guestRepository.findById(1L)).thenReturn(Optional.of(guest));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = reservationService.createReservation(
                1L, 1L, checkIn, checkOut, 2, BookingChannel.ONLINE_PORTAL);

        assertEquals(ReservationStatus.ACTIVE, result.getStatus());
        assertEquals(RoomStatus.RESERVED, room.getStatus()); // arriving today -> room held
        verify(roomRepository).save(room);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void createReservation_futureBookingLeavesRoomStatusAlone() {
        LocalDate checkIn = today.plusDays(20);
        LocalDate checkOut = today.plusDays(22);

        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.findOverlappingReservations(1L, checkIn, checkOut)).thenReturn(Collections.emptyList());
        when(guestRepository.findById(1L)).thenReturn(Optional.of(guest));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.createReservation(1L, 1L, checkIn, checkOut, 1, BookingChannel.PHONE);

        // A booking weeks away mustn't make the room look taken tonight.
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
        verify(roomRepository, never()).save(room);
    }

    @Test
    void quote_calculatesNightsTimesPriceAndFlagsCapacity() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.findOverlappingReservations(any(), any(), any())).thenReturn(Collections.emptyList());

        BookingQuote ok = reservationService.quote(1L, today.plusDays(1), today.plusDays(4), 2);
        assertEquals(3, ok.nights());
        assertEquals(150.0, ok.total()); // 3 nights x 50
        assertTrue(ok.ok());

        BookingQuote tooMany = reservationService.quote(1L, today.plusDays(1), today.plusDays(4), 5);
        assertFalse(tooMany.ok());
        assertEquals(150.0, tooMany.total()); // price is still shown
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
        assertNotNull(reservation.getCancelledAt());
    }

    @Test
    void cancelReservation_refusedOnceCheckedIn() {
        Reservation reservation = activeReservation(today.minusDays(1), today.plusDays(2));
        reservation.setCheckedInAt(today.minusDays(1).atTime(14, 0));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> reservationService.cancelReservation(5L));
    }

    @Test
    void cancelByGuest_allowedBeforeCheckIn() {
        Reservation reservation = activeReservation(today.plusDays(5), today.plusDays(7));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        reservationService.cancelByGuest(5L, 7L);

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }

    @Test
    void cancelByGuest_refusedForSomeoneElsesBooking() {
        Reservation reservation = activeReservation(today.plusDays(5), today.plusDays(7));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalArgumentException.class, () -> reservationService.cancelByGuest(5L, 99L));
        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
    }

    @Test
    void cancelByGuest_refusedAfterCheckInDateHasPassed() {
        Reservation reservation = activeReservation(today.minusDays(1), today.plusDays(1));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> reservationService.cancelByGuest(5L, 7L));
    }

    @Test
    void cancelByGuest_refusedOnceCheckedIn() {
        Reservation reservation = activeReservation(today, today.plusDays(2));
        reservation.setCheckedInAt(today.atTime(15, 0));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> reservationService.cancelByGuest(5L, 7L));
    }

    @Test
    void modifyReservation_rejectsPastCheckIn() {
        Reservation reservation = activeReservation(today.plusDays(5), today.plusDays(7));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalArgumentException.class, () ->
                reservationService.modifyReservation(5L, today.minusDays(2), today.plusDays(1)));
    }

    private Reservation activeReservation(LocalDate checkIn, LocalDate checkOut) {
        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoom(room);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);
        reservation.setNumberOfGuests(1);
        return reservation;
    }

    // Entities have no setId (the database assigns it), so tests set it by reflection.
    private static void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
