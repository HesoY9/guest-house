package com.hesoy9.guesthouse.repository;

import com.hesoy9.guesthouse.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // an invoice could have more than one payment (e.g. partial payments)
    List<Payment> findByInvoiceId(Long invoiceId);

    List<Payment> findByInvoiceIdOrderByIdAsc(Long invoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId")
    Double sumByInvoiceId(@Param("invoiceId") Long invoiceId);

    // invoice id -> amount paid so far, for every invoice in one query
    @Query("SELECT p.invoice.id, SUM(p.amount) FROM Payment p GROUP BY p.invoice.id")
    List<Object[]> sumPerInvoice();

    List<Payment> findByPaymentDateBetweenOrderByPaymentDateAscIdAsc(LocalDate from, LocalDate to);

    List<Payment> findTop10ByOrderByIdDesc();
}
