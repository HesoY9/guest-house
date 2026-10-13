package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Payment;
import com.hesoy9.guesthouse.entity.PaymentMethod;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.entity.ReservationStatus;
import com.hesoy9.guesthouse.service.BillingService;
import com.hesoy9.guesthouse.service.InvoiceSummary;
import com.hesoy9.guesthouse.service.PaymentService;
import com.hesoy9.guesthouse.service.ReservationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// Payments module (FR19-23): invoices, taking cash/card payments, printable invoice and receipt.
// Guests pay at the property, so this is where every payment is recorded.
@Controller
public class PaymentWebController {

    private static final String INVOICE_DRAWER = "staff/drawers/invoice :: drawer";

    private final BillingService billingService;
    private final PaymentService paymentService;
    private final ReservationService reservationService;

    public PaymentWebController(BillingService billingService, PaymentService paymentService,
                                ReservationService reservationService) {
        this.billingService = billingService;
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    // filter: unpaid (default) | all.  open: an invoice id whose panel should open on load.
    @GetMapping("/web/payments")
    public String payments(@RequestParam(defaultValue = "unpaid") String filter,
                           @RequestParam(required = false) Long open,
                           Model model) {
        var all = billingService.getAllSummaries();
        var shown = "all".equals(filter) ? all : all.stream().filter(s -> !s.settled()).toList();
        model.addAttribute("invoices", shown);
        model.addAttribute("filter", filter);
        model.addAttribute("open", open);
        model.addAttribute("outstanding", all.stream().mapToDouble(InvoiceSummary::balance).sum());
        model.addAttribute("unpaidCount", all.stream().filter(s -> !s.settled()).count());
        model.addAttribute("recentPayments", paymentService.getRecentPayments());
        return "staff/payments";
    }

    @GetMapping("/web/invoices/{id}")
    public String invoicePanel(@PathVariable Long id, Model model) {
        addInvoiceData(model, billingService.getInvoice(id));
        return INVOICE_DRAWER;
    }

    // Invoice before check-out (e.g. a guest settling up mid-stay). Check-out reuses it.
    @PostMapping("/web/invoices/for-reservation/{reservationId}")
    public String invoiceForReservation(@PathVariable Long reservationId, Model model, HttpServletResponse response) {
        Reservation reservation = reservationService.getReservation(reservationId);
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled reservations aren't invoiced");
        }
        Invoice invoice = billingService.getOrCreateInvoice(reservation);
        Hx.toastAndRefresh(response, "Invoice " + invoice.getNumber() + " ready.", null, null);
        addInvoiceData(model, invoice);
        return INVOICE_DRAWER;
    }

    // FR20-22: record a cash or card payment. The panel stays open showing the new balance,
    // and the toast links straight to the printable receipt (FR23, DR4).
    @PostMapping("/web/invoices/{id}/payments")
    public String recordPayment(@PathVariable Long id,
                                @RequestParam(required = false) Double amount,
                                @RequestParam(required = false) PaymentMethod method,
                                HttpSession session, Model model, HttpServletResponse response) {
        String staff = (String) session.getAttribute(AuthInterceptor.SESSION_USER);
        Payment payment = paymentService.recordPayment(id, amount, method, staff);
        Hx.toastAndRefresh(response, "Payment recorded - receipt " + payment.getReceiptNumber() + ".",
                "/web/payments/" + payment.getId() + "/receipt", "Print receipt");
        addInvoiceData(model, payment.getInvoice());
        return INVOICE_DRAWER;
    }

    @GetMapping("/web/invoices/{id}/print")
    public String printInvoice(@PathVariable Long id, Model model) {
        addInvoiceData(model, billingService.getInvoice(id));
        return "staff/print/invoice";
    }

    @GetMapping("/web/payments/{id}/receipt")
    public String printReceipt(@PathVariable Long id, Model model) {
        Payment payment = paymentService.getPayment(id);
        model.addAttribute("payment", payment);
        model.addAttribute("summary", billingService.summarize(payment.getInvoice()));
        return "staff/print/receipt";
    }

    private void addInvoiceData(Model model, Invoice invoice) {
        model.addAttribute("summary", billingService.summarize(invoice));
        model.addAttribute("payments", paymentService.getPaymentsForInvoice(invoice.getId()));
        model.addAttribute("methods", PaymentMethod.values());
    }
}
