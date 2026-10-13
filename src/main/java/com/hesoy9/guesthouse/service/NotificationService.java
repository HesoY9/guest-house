package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

// Booking confirmation and cancellation emails to guests.
// With no MAIL_HOST configured, the email is written to the log instead - handy in development,
// and booking never fails just because email isn't set up. Sending happens in the background
// so a slow mail server never slows down the booking page.
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String mailHost;
    private final String from;
    private final String baseUrl;

    public NotificationService(ObjectProvider<JavaMailSender> mailSender,
                               @Value("${spring.mail.host:}") String mailHost,
                               @Value("${app.mail.from:}") String from,
                               @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.from = from;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public boolean isEmailEnabled() {
        return mailHost != null && !mailHost.isBlank();
    }

    public void bookingConfirmed(Reservation r) {
        String body = "Dear " + r.getGuest().getName() + ",\n\n"
                + "Thank you for booking with Beachside Heaven. Your reservation is confirmed.\n\n"
                + details(r)
                + "Payment: pay at the property on arrival or departure - cash or card.\n"
                + "Please bring your ID or passport for check-in.\n\n"
                + "Need to cancel? You can cancel online from My Bookings any time before check-in:\n"
                + baseUrl + "/my-bookings\n\n"
                + "Printable confirmation: " + baseUrl + "/my-bookings/" + r.getId() + "\n\n"
                + "We look forward to welcoming you.\nBeachside Heaven";
        send(r.getGuest().getEmail(), "Booking confirmed - " + r.getReference(), body);
    }

    public void bookingCancelled(Reservation r) {
        String body = "Dear " + r.getGuest().getName() + ",\n\n"
                + "Your reservation " + r.getReference() + " has been cancelled.\n\n"
                + details(r)
                + "If this wasn't you, or you'd like to book again, visit " + baseUrl + "\n\n"
                + "Beachside Heaven";
        send(r.getGuest().getEmail(), "Booking cancelled - " + r.getReference(), body);
    }

    private String details(Reservation r) {
        return "Booking reference: " + r.getReference() + "\n"
                + "Room: " + r.getRoom().getType() + " - Room " + r.getRoom().getRoomNumber() + "\n"
                + "Check-in: " + DATE.format(r.getCheckInDate()) + "\n"
                + "Check-out: " + DATE.format(r.getCheckOutDate()) + "\n"
                + "Guests: " + r.getNumberOfGuests() + "\n"
                + String.format("Total: %d night(s) x LKR %,.2f = LKR %,.2f%n%n",
                        r.getNights(), r.getRoom().getPrice(), r.getEstimatedTotal());
    }

    private void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return; // e.g. a walk-in guest with no email on file
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!isEmailEnabled() || sender == null) {
            log.info("Email not sent (MAIL_HOST not configured). To: {} | Subject: {}\n{}", to, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        CompletableFuture.runAsync(() -> {
            try {
                sender.send(message);
                log.info("Email sent to {}: {}", to, subject);
            } catch (Exception ex) {
                log.warn("Could not send email to {}: {}", to, ex.getMessage());
            }
        });
    }
}
