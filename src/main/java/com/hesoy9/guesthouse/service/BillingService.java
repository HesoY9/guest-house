package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class BillingService {

    private final InvoiceRepository invoiceRepository;

    public BillingService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    // FR17, FR19: nights stayed x room price per night
    public Invoice generateInvoice(Reservation reservation) {
        long nights = ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate());
        double totalAmount = nights * reservation.getRoom().getPrice();

        Invoice invoice = new Invoice();
        invoice.setReservation(reservation);
        invoice.setTotalAmount(totalAmount);
        invoice.setGeneratedDate(LocalDate.now());

        return invoiceRepository.save(invoice);
    }
}
