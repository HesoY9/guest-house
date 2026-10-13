package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.entity.ReservationStatus;
import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.entity.RoomStatus;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

// "What needs doing today" for the front desk.
@Service
public class DashboardService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final BillingService billingService;

    public DashboardService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            BillingService billingService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.billingService = billingService;
    }

    public Today today() {
        LocalDate today = LocalDate.now();
        List<Room> rooms = roomRepository.findAllByOrderByRoomNumberAsc();
        List<Reservation> inHouse = reservationRepository.findInHouse();
        List<InvoiceSummary> unpaid = billingService.getUnpaidSummaries();

        // Rooms with a guest staying tonight (checked in, or still due to arrive today)
        long occupiedTonight = reservationRepository.findStaysOverlapping(today, today.plusDays(1)).stream()
                .filter(r -> r.getStatus() == ReservationStatus.ACTIVE)
                .filter(r -> r.getCheckOutDate().isAfter(today))
                .count();

        return new Today(today,
                reservationRepository.findArrivals(today),
                reservationRepository.findDepartures(today),
                inHouse,
                rooms.stream().filter(r -> r.getStatus() == RoomStatus.CLEANING_REQUIRED).toList(),
                unpaid,
                unpaid.stream().mapToDouble(InvoiceSummary::balance).sum(),
                rooms.size(),
                rooms.isEmpty() ? 0 : occupiedTonight * 100.0 / rooms.size());
    }

    public record Today(LocalDate date,
                        List<Reservation> arrivals,
                        List<Reservation> departures,
                        List<Reservation> inHouse,
                        List<Room> roomsToClean,
                        List<InvoiceSummary> unpaidInvoices,
                        double outstanding,
                        int roomCount,
                        double occupancyTonight) {
    }
}
