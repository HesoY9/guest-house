package com.hesoy9.guesthouse.config;

import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Display formatting shared by every template, so money and dates look the same everywhere.
// Use from Thymeleaf as ${@fmt.lkr(room.price)}, ${@fmt.date(r.checkInDate)}, ...
@Component("fmt")
public class Formats {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_NAME = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    // "LKR 12,500" - decimals only when there are cents, e.g. "LKR 12,500.50"
    public String lkr(Number amount) {
        return "LKR " + number(amount);
    }

    public String number(Number amount) {
        if (amount == null) return "0";
        double value = amount.doubleValue();
        boolean whole = Math.abs(value - Math.rint(value)) < 0.005;
        DecimalFormat format = new DecimalFormat(whole ? "#,##0" : "#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return format.format(value);
    }

    public String percent(Number value) {
        return value == null ? "0%" : Math.round(value.doubleValue()) + "%";
    }

    public String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public String longDate(LocalDate date) {
        return date == null ? "" : LONG_DATE.format(date);
    }

    public String shortDate(LocalDate date) {
        return date == null ? "" : SHORT_DATE.format(date);
    }

    public String month(LocalDate date) {
        return date == null ? "" : MONTH.format(date);
    }

    public String dayName(LocalDate date) {
        return date == null ? "" : DAY_NAME.format(date);
    }

    public String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME.format(dateTime);
    }

    public String plural(Number count, String singular, String plural) {
        long n = count == null ? 0 : count.longValue();
        return n + " " + (n == 1 ? singular : plural);
    }

    // Same as RoomPhoto.getSrc() - kept for templates that format a raw URL.
    public String photo(String url) {
        return com.hesoy9.guesthouse.entity.RoomPhoto.toSrc(url);
    }
}
