package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Payment;
import com.hesoy9.guesthouse.entity.PaymentMethod;
import com.hesoy9.guesthouse.repository.InvoiceRepository;
import com.hesoy9.guesthouse.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    // FR20-23: the saved Payment record is your receipt data - format it for display in the controller
    public Payment recordPayment(Long invoiceId, Double amount, PaymentMethod method) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setPaymentDate(LocalDate.now());

        return paymentRepository.save(payment);
    }
}
