package com.campusconnect.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportInput(

        @NotBlank(message = "Le motif est obligatoire")
        @Size(min = 5, max = 500, message = "Le motif doit contenir entre 5 et 500 caractères")
        String reason) {
}
