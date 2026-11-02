package com.campusconnect.backend.dto;

import java.time.Instant;

import com.campusconnect.backend.entity.Report;
import com.campusconnect.backend.entity.ReportStatus;

public record ReportResponse(
        Long id,
        Long requestId,
        String reason,
        ReportStatus status,
        Instant createdAt,
        Instant resolvedAt,
        AuthorSummary reporter) {

    /**
     * À appeler pendant une transaction : le signaleur et la demande sont chargés à la demande.
     */
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getRequest().getId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getResolvedAt(),
                new AuthorSummary(report.getReporter().getId(), report.getReporter().getFullName()));
    }
}
