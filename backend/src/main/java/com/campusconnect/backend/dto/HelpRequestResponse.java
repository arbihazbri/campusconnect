package com.campusconnect.backend.dto;

import java.time.Instant;

import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.RequestStatus;

public record HelpRequestResponse(
        Long id,
        String title,
        String description,
        String availability,
        Level level,
        RequestStatus status,
        Instant createdAt,
        SubjectResponse subject,
        AuthorSummary author,
        boolean ownedByCurrentUser) {

    /**
     * À appeler pendant une transaction : l'auteur et la matière sont chargés à la demande.
     */
    public static HelpRequestResponse from(HelpRequest request, Long currentUserId) {
        Long authorId = request.getAuthor().getId();
        return new HelpRequestResponse(
                request.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getAvailability(),
                request.getLevel(),
                request.getStatus(),
                request.getCreatedAt(),
                SubjectResponse.from(request.getSubject()),
                new AuthorSummary(authorId, request.getAuthor().getFullName()),
                authorId.equals(currentUserId));
    }
}