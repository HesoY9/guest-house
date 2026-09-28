package com.hesoy9.guesthouse.controller;

import com.hesoy9.guesthouse.dto.PaymentRequest;
import com.hesoy9.guesthouse.entity.Payment;
import com.hesoy9.guesthouse.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> recordPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.recordPayment(
                request.getInvoiceId(), request.getAmount(), request.getMethod());
        return ResponseEntity.ok(payment);
    }
}
