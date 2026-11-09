package com.campusconnect.backend.dto;

import java.util.List;

/**
 * Demandes recommandées à un étudiant selon ses compétences (fonctionnalité originale).
 *
 * @param hasSkills false si le profil ne déclare aucune compétence : l'interface invite alors à le compléter
 */
public record RecommendationsResponse(boolean hasSkills, List<Recommendation> items) {

    public RecommendationsResponse {
        items = List.copyOf(items);
    }

    /** @param sameLevel true si la demande est du même niveau que l'étudiant (affichée en priorité) */
    public record Recommendation(HelpRequestResponse request, boolean sameLevel) {
    }
}
