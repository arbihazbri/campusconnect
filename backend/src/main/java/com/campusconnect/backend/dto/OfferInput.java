package com.campusconnect.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OfferInput(

        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 5, max = 500, message = "Le message doit contenir entre 5 et 500 caractères")
        String message) {
}