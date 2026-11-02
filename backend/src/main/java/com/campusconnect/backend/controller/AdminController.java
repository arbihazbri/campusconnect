package com.campusconnect.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.backend.dto.ReportedRequestResponse;
import com.campusconnect.backend.service.ReportService;

/**
 * Espace administrateur : gestion des publications signalées.
 * L'accès à /api/admin/** est réservé au rôle ADMIN dans SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ReportService reportService;

    public AdminController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Publications signalées. Par défaut, seules celles qui ont encore un signalement en attente ;
     * {@code ?pendingOnly=false} renvoie aussi l'historique (signalements traités ou rejetés).
     */
    @GetMapping("/reports")
    public List<ReportedRequestResponse> reportedRequests(
            @RequestParam(name = "pendingOnly", defaultValue = "true") boolean pendingOnly) {
        return reportService.listReportedRequests(pendingOnly);
    }

    @PostMapping("/requests/{id}/hide")
    public ReportedRequestResponse hide(@PathVariable("id") Long id) {
        return reportService.hide(id);
    }

    @PostMapping("/requests/{id}/restore")
    public ReportedRequestResponse restore(@PathVariable("id") Long id) {
        return reportService.restore(id);
    }

    @PostMapping("/requests/{id}/dismiss")
    public ReportedRequestResponse dismiss(@PathVariable("id") Long id) {
        return reportService.dismiss(id);
    }
}
