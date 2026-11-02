package com.campusconnect.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.backend.dto.ReportInput;
import com.campusconnect.backend.dto.ReportResponse;
import com.campusconnect.backend.service.ReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/requests/{requestId}/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse report(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody ReportInput input) {
        return reportService.report(requestId, currentUserId(jwt), input);
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
