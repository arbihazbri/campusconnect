package com.campusconnect.backend.dto;

import com.campusconnect.backend.entity.Level;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HelpRequestInput(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 5, max = 100, message = "Le titre doit contenir entre 5 et 100 caractères")
        String title,

        @NotBlank(message = "La description est obligatoire")
        @Size(min = 10, max = 2000, message = "La description doit contenir entre 10 et 2000 caractères")
        String description,

        @NotBlank(message = "La disponibilité est obligatoire")
        @Size(max = 255, message = "La disponibilité ne doit pas dépasser 255 caractères")
        String availability,

        @NotNull(message = "La matière est obligatoire")
        Long subjectId,

        @NotNull(message = "Le niveau est obligatoire")
        Level level) {
}