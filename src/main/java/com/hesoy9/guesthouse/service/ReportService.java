package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.entity.RoomStatus;
import com.hesoy9.guesthouse.repository.InvoiceRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;

    public ReportService(RoomRepository roomRepository, InvoiceRepository invoiceRepository) {
        this.roomRepository = roomRepository;
        this.invoiceRepository = invoiceRepository;
    }

    // FR26 - % of rooms currently occupied
    public double getOccupancyRate() {
        List<Room> allRooms = roomRepository.findAll();
        if (allRooms.isEmpty()) return 0.0;

        long occupiedCount = allRooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.OCCUPIED)
                .count();

        return (occupiedCount * 100.0) / allRooms.size();
    }

    // FR27 - total income between two dates
    // Needs one extra repository method - see note below.
    public double getIncomeBetween(LocalDate from, LocalDate to) {
        List<Invoice> invoices = invoiceRepository.findByGeneratedDateBetween(from, to);
        return invoices.stream().mapToDouble(Invoice::getTotalAmount).sum();
    }
}
