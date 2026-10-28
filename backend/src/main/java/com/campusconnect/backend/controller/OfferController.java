package com.campusconnect.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.backend.dto.OfferInput;
import com.campusconnect.backend.dto.OfferResponse;
import com.campusconnect.backend.service.OfferService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/requests/{requestId}/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody OfferInput input) {
        return offerService.create(requestId, currentUserId(jwt), input);
    }

    @GetMapping
    public List<OfferResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("requestId") Long requestId) {
        return offerService.listForRequest(requestId, currentUserId(jwt));
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}