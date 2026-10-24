package com.campusconnect.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Le format de l'email est invalide")
        @Size(max = 255, message = "L'email ne doit pas dépasser 255 caractères")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 64, message = "Le mot de passe doit contenir entre 8 et 64 caractères")
        String password,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 150, message = "Le nom doit contenir entre 2 et 150 caractères")
        String fullName) {
}