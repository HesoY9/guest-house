package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Invoice;

// An invoice together with how much has been paid against it.
public record InvoiceSummary(Invoice invoice, double paid) {

    public double total() {
        return invoice.getTotalAmount();
    }

    public double balance() {
        return Math.max(0, Math.round((invoice.getTotalAmount() - paid) * 100) / 100.0);
    }

    public boolean settled() {
        return balance() <= 0.009;
    }

    public String state() {
        if (settled()) return "Paid";
        return paid > 0 ? "Part paid" : "Unpaid";
    }

    public String tone() {
        if (settled()) return "success";
        return paid > 0 ? "warning" : "danger";
    }
}
