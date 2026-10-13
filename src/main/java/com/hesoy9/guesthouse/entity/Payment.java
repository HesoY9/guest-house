package com.hesoy9.guesthouse.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentMethod method;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    // Staff username that took the payment - shown on the receipt and in the daily report.
    @Column(name = "received_by", length = 50)
    private String receivedBy;

    public Payment() {
        // required by JPA
    }

    public Payment(Invoice invoice, Double amount, PaymentMethod method, LocalDate paymentDate) {
        this.invoice = invoice;
        this.amount = amount;
        this.method = method;
        this.paymentDate = paymentDate;
    }

    public Long getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    // Receipt number printed on the receipt (DR4), e.g. "RC-000017".
    public String getReceiptNumber() {
        return id == null ? "" : String.format("RC-%06d", id);
    }
}
