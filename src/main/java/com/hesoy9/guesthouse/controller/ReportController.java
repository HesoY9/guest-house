package com.hesoy9.guesthouse.controller;

import com.hesoy9.guesthouse.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/occupancy")
    public ResponseEntity<Double> getOccupancyRate() {
        return ResponseEntity.ok(reportService.getOccupancyRate());
    }

    // e.g. GET /api/reports/income?from=2026-01-01&to=2026-01-31
    @GetMapping("/income")
    public ResponseEntity<Double> getIncome(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ResponseEntity.ok(reportService.getIncomeBetween(from, to));
    }
}
