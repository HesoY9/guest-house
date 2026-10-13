package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.*;
import com.hesoy9.guesthouse.repository.InvoiceRepository;
import com.hesoy9.guesthouse.repository.PaymentRepository;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReportService {

    // Keeps a report page quick and readable - about a quarter.
    static final int MAX_DAYS = 92;

    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    public ReportService(RoomRepository roomRepository, InvoiceRepository invoiceRepository,
                         ReservationRepository reservationRepository, PaymentRepository paymentRepository) {
        this.roomRepository = roomRepository;
        this.invoiceRepository = invoiceRepository;
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
    }

    // FR26 - % of rooms currently occupied (snapshot, used by the REST API)
    public double getOccupancyRate() {
        List<Room> allRooms = roomRepository.findAll();
        if (allRooms.isEmpty()) return 0.0;

        long occupiedCount = allRooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.OCCUPIED)
                .count();

        return (occupiedCount * 100.0) / allRooms.size();
    }

    // FR27 - total invoiced between two dates (used by the REST API)
    public double getIncomeBetween(LocalDate from, LocalDate to) {
        List<Invoice> invoices = invoiceRepository.findByGeneratedDateBetween(from, to);
        return invoices.stream().mapToDouble(Invoice::getTotalAmount).sum();
    }

    // Everything the Reports page shows for the period [from, to] (both days included):
    // FR26 occupancy, FR27 income, FR28 reservations, FR29 daily transactions.
    public PeriodReport buildReport(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Choose a valid date range");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new IllegalArgumentException("Reports can cover at most " + MAX_DAYS + " days");
        }
        LocalDate end = to.plusDays(1); // exclusive upper bound for night counting
        int roomCount = (int) roomRepository.count();
        List<Reservation> stays = reservationRepository.findStaysOverlapping(from, end);
        List<Payment> payments = paymentRepository.findByPaymentDateBetweenOrderByPaymentDateAscIdAsc(from, to);

        // Per-day rows
        Map<LocalDate, DayRow> days = new LinkedHashMap<>();
        for (LocalDate d = from; d.isBefore(end); d = d.plusDays(1)) {
            days.put(d, new DayRow(d));
        }
        long nightsSold = 0;
        double roomRevenue = 0;
        for (Reservation r : stays) {
            LocalDate start = r.getCheckInDate().isBefore(from) ? from : r.getCheckInDate();
            LocalDate stop = r.getCheckOutDate().isAfter(end) ? end : r.getCheckOutDate();
            for (LocalDate d = start; d.isBefore(stop); d = d.plusDays(1)) {
                days.get(d).occupiedRooms++;
                nightsSold++;
                roomRevenue += r.getRoom().getPrice();
            }
            if (days.containsKey(r.getCheckInDate())) days.get(r.getCheckInDate()).arrivals++;
            if (days.containsKey(r.getCheckOutDate())) days.get(r.getCheckOutDate()).departures++;
        }
        for (Payment p : payments) {
            DayRow row = days.get(p.getPaymentDate());
            if (row != null) {
                row.payments++;
                row.takings += p.getAmount();
                if (p.getMethod() == PaymentMethod.CASH) row.cash += p.getAmount(); else row.card += p.getAmount();
            }
        }

        long nightsAvailable = (long) roomCount * days.size();
        double occupancy = nightsAvailable == 0 ? 0 : nightsSold * 100.0 / nightsAvailable;
        double cash = payments.stream().filter(p -> p.getMethod() == PaymentMethod.CASH).mapToDouble(Payment::getAmount).sum();
        double card = payments.stream().filter(p -> p.getMethod() == PaymentMethod.CARD).mapToDouble(Payment::getAmount).sum();
        double invoiced = getIncomeBetween(from, to);

        // FR28 - reservations arriving in the period, broken down by status and channel
        List<Reservation> arriving = reservationRepository.findByCheckInDateBetweenOrderByCheckInDate(from, to);
        Map<ReservationStatus, Long> byStatus = new EnumMap<>(ReservationStatus.class);
        Map<BookingChannel, Long> byChannel = new EnumMap<>(BookingChannel.class);
        for (ReservationStatus s : ReservationStatus.values()) byStatus.put(s, 0L);
        for (BookingChannel c : BookingChannel.values()) byChannel.put(c, 0L);
        for (Reservation r : arriving) {
            byStatus.merge(r.getStatus(), 1L, Long::sum);
            byChannel.merge(r.getBookingChannel(), 1L, Long::sum);
        }

        return new PeriodReport(from, to, roomCount, nightsSold, nightsAvailable, occupancy,
                roomRevenue, nightsSold == 0 ? 0 : roomRevenue / nightsSold,
                cash + card, cash, card, invoiced,
                arriving, byStatus, byChannel, new ArrayList<>(days.values()), payments);
    }

    public record PeriodReport(LocalDate from, LocalDate to, int roomCount,
                               long nightsSold, long nightsAvailable, double occupancyPercent,
                               double roomRevenue, double averageDailyRate,
                               double takings, double cashTakings, double cardTakings, double invoiced,
                               List<Reservation> reservations,
                               Map<ReservationStatus, Long> reservationsByStatus,
                               Map<BookingChannel, Long> reservationsByChannel,
                               List<DayRow> days,
                               List<Payment> payments) {
    }

    // One line of the daily report. Mutable while the report is being added up.
    public static final class DayRow {
        private final LocalDate date;
        private int occupiedRooms;
        private int arrivals;
        private int departures;
        private int payments;
        private double takings;
        private double cash;
        private double card;

        DayRow(LocalDate date) {
            this.date = date;
        }

        public LocalDate getDate() { return date; }
        public int getOccupiedRooms() { return occupiedRooms; }
        public int getArrivals() { return arrivals; }
        public int getDepartures() { return departures; }
        public int getPayments() { return payments; }
        public double getTakings() { return takings; }
        public double getCash() { return cash; }
        public double getCard() { return card; }
    }
}
