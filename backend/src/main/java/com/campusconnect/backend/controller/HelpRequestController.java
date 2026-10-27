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

import com.campusconnect.backend.dto.HelpRequestInput;
import com.campusconnect.backend.dto.HelpRequestResponse;
import com.campusconnect.backend.service.HelpRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/requests")
public class HelpRequestController {

    private final HelpRequestService helpRequestService;

    public HelpRequestController(HelpRequestService helpRequestService) {
        this.helpRequestService = helpRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HelpRequestResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody HelpRequestInput input) {
        return helpRequestService.create(currentUserId(jwt), input);
    }

    @GetMapping("/{id}")
    public HelpRequestResponse getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return helpRequestService.getById(id, currentUserId(jwt));
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}