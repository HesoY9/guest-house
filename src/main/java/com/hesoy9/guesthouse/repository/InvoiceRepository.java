package com.hesoy9.guesthouse.repository;

import com.hesoy9.guesthouse.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // one invoice per reservation (OneToOne on the entity)
    Optional<Invoice> findByReservationId(Long reservationId);

    List<Invoice> findByGeneratedDateBetween(LocalDate from, LocalDate to);

    List<Invoice> findAllByOrderByIdDesc();

    List<Invoice> findByReservationGuestId(Long guestId);
}
