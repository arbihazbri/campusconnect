package com.campusconnect.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "L'email est obligatoire")
        @Size(max = 255, message = "L'email est trop long")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(max = 64, message = "Le mot de passe est trop long")
        String password) {
}