package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.service.CalendarService;
import com.hesoy9.guesthouse.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class DashboardWebController {

    private final DashboardService dashboardService;
    private final CalendarService calendarService;

    public DashboardWebController(DashboardService dashboardService, CalendarService calendarService) {
        this.dashboardService = dashboardService;
        this.calendarService = calendarService;
    }

    // Today's arrivals, departures, rooms to clean and unpaid invoices.
    @GetMapping("/web/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("day", dashboardService.today());
        return "staff/dashboard";
    }

    // The room calendar ("tape chart"): rooms down the side, dates across the top.
    @GetMapping("/web/calendar")
    public String calendar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                           @RequestParam(defaultValue = "14") int days,
                           Model model) {
        int span = Math.clamp(days, 7, 31);
        LocalDate from = start != null ? start : LocalDate.now().minusDays(1);
        model.addAttribute("cal", calendarService.build(from, span));
        return "staff/calendar";
    }
}
