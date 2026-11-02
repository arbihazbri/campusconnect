package com.campusconnect.backend.dto;

import java.time.Instant;

import com.campusconnect.backend.entity.Rating;

public record RatingResponse(
        Long id,
        Long requestId,
        int score,
        String comment,
        Instant createdAt,
        AuthorSummary rater,
        AuthorSummary rated) {

    /**
     * À appeler pendant une transaction : l'évaluateur, l'évalué et la demande sont chargés à la demande.
     */
    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getRequest().getId(),
                rating.getScore(),
                rating.getComment(),
                rating.getCreatedAt(),
                new AuthorSummary(rating.getRater().getId(), rating.getRater().getFullName()),
                new AuthorSummary(rating.getRated().getId(), rating.getRated().getFullName()));
    }
}
