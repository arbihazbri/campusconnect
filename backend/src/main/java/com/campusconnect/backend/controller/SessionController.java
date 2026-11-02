package com.campusconnect.backend.controller;

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

import com.campusconnect.backend.dto.HelpRequestResponse;
import com.campusconnect.backend.dto.RatingInput;
import com.campusconnect.backend.dto.RatingResponse;
import com.campusconnect.backend.dto.SessionRatingsResponse;
import com.campusconnect.backend.service.SessionService;

import jakarta.validation.Valid;

/**
 * Suivi de la séance d'une demande : clôture (AIDE_ACCEPTEE → TERMINEE) puis évaluations.
 */
@RestController
@RequestMapping("/api/requests/{requestId}")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/complete")
    public HelpRequestResponse complete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("requestId") Long requestId) {
        return sessionService.complete(requestId, currentUserId(jwt));
    }

    @PostMapping("/ratings")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse rate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody RatingInput input) {
        return sessionService.rate(requestId, currentUserId(jwt), input);
    }

    @GetMapping("/ratings")
    public SessionRatingsResponse ratings(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("requestId") Long requestId) {
        return sessionService.ratings(requestId, currentUserId(jwt));
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
