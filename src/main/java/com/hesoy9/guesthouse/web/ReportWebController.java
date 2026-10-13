package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

// Reports (FR26-29) for managers: occupancy, income, reservations and daily transactions.
@Controller
public class ReportWebController {

    private final ReportService reportService;

    public ReportWebController(ReportService reportService) {
        this.reportService = reportService;
    }

    // Defaults to this month so far.
    @GetMapping("/web/reports")
    public String reports(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model) {
        LocalDate today = LocalDate.now();
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : today;
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        try {
            model.addAttribute("report", reportService.buildReport(start, end));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "staff/reports";
    }
}
