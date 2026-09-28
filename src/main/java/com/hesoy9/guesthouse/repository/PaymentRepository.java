package com.hesoy9.guesthouse.repository;

import com.hesoy9.guesthouse.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // an invoice could have more than one payment (e.g. partial payments)
    List<Payment> findByInvoiceId(Long invoiceId);
}
