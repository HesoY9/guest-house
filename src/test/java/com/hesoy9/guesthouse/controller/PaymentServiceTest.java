package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Payment;
import com.hesoy9.guesthouse.entity.PaymentMethod;
import com.hesoy9.guesthouse.repository.InvoiceRepository;
import com.hesoy9.guesthouse.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoice = new Invoice(null, 30000.0, LocalDate.now());
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
    }

    @Test
    void recordsPartPaymentWithStaffName() {
        when(paymentRepository.sumByInvoiceId(1L)).thenReturn(0.0);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment payment = paymentService.recordPayment(1L, 10000.0, PaymentMethod.CASH, "reception1");

        assertEquals(10000.0, payment.getAmount());
        assertEquals(PaymentMethod.CASH, payment.getMethod());
        assertEquals("reception1", payment.getReceivedBy());
        assertEquals(LocalDate.now(), payment.getPaymentDate());
    }

    @Test
    void refusesMoreThanTheBalance() {
        when(paymentRepository.sumByInvoiceId(1L)).thenReturn(25000.0); // 5,000 left

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.recordPayment(1L, 6000.0, PaymentMethod.CARD, "x"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refusesPaymentOnSettledInvoice() {
        when(paymentRepository.sumByInvoiceId(1L)).thenReturn(30000.0);

        assertThrows(IllegalStateException.class,
                () -> paymentService.recordPayment(1L, 100.0, PaymentMethod.CASH, "x"));
    }

    @Test
    void refusesZeroOrMissingMethod() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.recordPayment(1L, 0.0, PaymentMethod.CASH, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.recordPayment(1L, 100.0, null, "x"));
    }
}
