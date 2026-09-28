package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.*;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInOutServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BillingService billingService;

    @InjectMocks
    private CheckInOutService checkInOutService;

    @Test
    void checkIn_throwsWhenGuestHasNoId() {
        Guest guest = new Guest();
        guest.setIdOrPassport(""); // blank - not allowed (DR3)

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> checkInOutService.checkIn(1L));
    }

    @Test
    void checkIn_marksRoomOccupiedWhenGuestHasId() {
        Guest guest = new Guest();
        guest.setIdOrPassport("P99999");

        Room room = new Room();
        room.setStatus(RoomStatus.RESERVED);

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoom(room);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        checkInOutService.checkIn(1L);

        assertEquals(RoomStatus.OCCUPIED, room.getStatus()); // FR15
        verify(roomRepository).save(room);
    }

    @Test
    void checkOut_marksRoomCleaningRequiredAndGeneratesInvoice() {
        Room room = new Room();
        room.setStatus(RoomStatus.OCCUPIED);

        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setStatus(ReservationStatus.ACTIVE);

        Invoice expectedInvoice = new Invoice();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(billingService.generateInvoice(reservation)).thenReturn(expectedInvoice);

        Invoice result = checkInOutService.checkOut(1L);

        assertEquals(ReservationStatus.COMPLETED, reservation.getStatus());
        assertEquals(RoomStatus.CLEANING_REQUIRED, room.getStatus()); // FR18
        assertSame(expectedInvoice, result);
    }
}
