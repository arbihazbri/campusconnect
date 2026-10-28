package com.campusconnect.backend.dto;

import java.time.Instant;

import com.campusconnect.backend.entity.HelpOffer;
import com.campusconnect.backend.entity.OfferStatus;

public record OfferResponse(
        Long id,
        Long requestId,
        String message,
        OfferStatus status,
        Instant createdAt,
        AuthorSummary helper) {

    /**
     * À appeler pendant une transaction : l'aidant et la demande sont chargés à la demande.
     */
    public static OfferResponse from(HelpOffer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getRequest().getId(),
                offer.getMessage(),
                offer.getStatus(),
                offer.getCreatedAt(),
                new AuthorSummary(offer.getHelper().getId(), offer.getHelper().getFullName()));
    }
}