package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Payment;
import com.hesoy9.guesthouse.entity.PaymentMethod;
import com.hesoy9.guesthouse.repository.InvoiceRepository;
import com.hesoy9.guesthouse.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public Payment recordPayment(Long invoiceId, Double amount, PaymentMethod method) {
        return recordPayment(invoiceId, amount, method, null);
    }

    // FR20-23: the saved Payment record is the receipt data (DR4) - see the receipt print page.
    @Transactional
    public Payment recordPayment(Long invoiceId, Double amount, PaymentMethod method, String receivedBy) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (method == null) {
            throw new IllegalArgumentException("Choose cash or card"); // FR21, FR22
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        double balance = invoice.getTotalAmount() - paymentRepository.sumByInvoiceId(invoiceId);
        if (balance <= 0.009) {
            throw new IllegalStateException("This invoice is already fully paid");
        }
        if (amount > balance + 0.009) {
            throw new IllegalArgumentException(String.format(
                    "That's more than the balance due (LKR %,.2f)", balance));
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setPaymentDate(LocalDate.now());
        payment.setReceivedBy(receivedBy);

        return paymentRepository.save(payment);
    }

    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    public List<Payment> getPaymentsForInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceIdOrderByIdAsc(invoiceId);
    }

    public List<Payment> getRecentPayments() {
        return paymentRepository.findTop10ByOrderByIdDesc();
    }
}
