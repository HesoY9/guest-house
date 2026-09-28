package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private BillingService billingService;

    @Test
    void generateInvoice_calculatesNightsTimesPrice() {
        Room room = new Room();
        room.setPrice(50.0);

        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setCheckInDate(LocalDate.of(2026, 1, 10));
        reservation.setCheckOutDate(LocalDate.of(2026, 1, 13)); // 3 nights

        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Invoice invoice = billingService.generateInvoice(reservation);

        assertEquals(150.0, invoice.getTotalAmount()); // 3 nights x 50/night
    }

    @Test
    void generateInvoice_oneNightStay() {
        Room room = new Room();
        room.setPrice(75.0);

        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setCheckInDate(LocalDate.of(2026, 2, 1));
        reservation.setCheckOutDate(LocalDate.of(2026, 2, 2)); // 1 night

        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Invoice invoice = billingService.generateInvoice(reservation);

        assertEquals(75.0, invoice.getTotalAmount());
    }
}
