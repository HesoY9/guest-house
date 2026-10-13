package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.repository.InvoiceRepository;
import com.hesoy9.guesthouse.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public BillingService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
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

    // One invoice per reservation: returns the existing one, or creates it. Lets the desk take
    // payment during the stay (pay-at-property) and still check out without a duplicate invoice.
    @Transactional
    public Invoice getOrCreateInvoice(Reservation reservation) {
        return invoiceRepository.findByReservationId(reservation.getId())
                .orElseGet(() -> generateInvoice(reservation));
    }

    public Invoice getInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
    }

    public InvoiceSummary summarize(Invoice invoice) {
        return new InvoiceSummary(invoice, paymentRepository.sumByInvoiceId(invoice.getId()));
    }

    // Newest first, each with its paid amount - for the Payments page.
    public List<InvoiceSummary> getAllSummaries() {
        Map<Long, Double> paid = paidPerInvoice();
        return invoiceRepository.findAllByOrderByIdDesc().stream()
                .map(inv -> new InvoiceSummary(inv, paid.getOrDefault(inv.getId(), 0.0)))
                .toList();
    }

    public List<InvoiceSummary> getUnpaidSummaries() {
        return getAllSummaries().stream().filter(s -> !s.settled()).toList();
    }

    public List<InvoiceSummary> getGuestSummaries(Long guestId) {
        Map<Long, Double> paid = paidPerInvoice();
        return invoiceRepository.findByReservationGuestId(guestId).stream()
                .map(inv -> new InvoiceSummary(inv, paid.getOrDefault(inv.getId(), 0.0)))
                .toList();
    }

    private Map<Long, Double> paidPerInvoice() {
        Map<Long, Double> paid = new HashMap<>();
        for (Object[] row : paymentRepository.sumPerInvoice()) {
            paid.put((Long) row[0], ((Number) row[1]).doubleValue());
        }
        return paid;
    }
}
