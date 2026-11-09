package com.campusconnect.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.backend.dto.RecommendationsResponse;
import com.campusconnect.backend.service.RecommendationService;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public RecommendationsResponse recommendations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "limit", defaultValue = "6") int limit) {
        return recommendationService.forUser(Long.valueOf(jwt.getSubject()), limit);
    }
}
