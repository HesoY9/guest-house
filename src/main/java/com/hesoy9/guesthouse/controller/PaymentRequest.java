package com.hesoy9.guesthouse.dto;

import com.hesoy9.guesthouse.entity.PaymentMethod;

public class PaymentRequest {

    private Long invoiceId;
    private Double amount;
    private PaymentMethod method;

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
}
