package com.campusconnect.backend.dto;

/**
 * Reconnaissance des contributions d'un étudiant (fonctionnalité originale).
 *
 * @param averageRating  note moyenne reçue (1 à 5, une décimale), null sans évaluation
 * @param ratingsCount   nombre d'évaluations reçues
 * @param sessionsHelped nombre de séances terminées où l'étudiant était l'aidant
 * @param badge          NOUVEAU (aucune aide terminée), CONTRIBUTEUR (au moins une), MENTOR (5 aides et moyenne ≥ 4)
 */
public record Reputation(Double averageRating, long ratingsCount, long sessionsHelped, Badge badge) {

    public static final int MENTOR_SESSIONS = 5;
    public static final double MENTOR_AVERAGE = 4.0;

    public enum Badge {
        NOUVEAU,
        CONTRIBUTEUR,
        MENTOR
    }

    public static Reputation of(Double averageRating, long ratingsCount, long sessionsHelped) {
        Double rounded = averageRating == null ? null : Math.round(averageRating * 10) / 10.0;
        Badge badge;
        if (sessionsHelped >= MENTOR_SESSIONS && rounded != null && rounded >= MENTOR_AVERAGE) {
            badge = Badge.MENTOR;
        } else if (sessionsHelped > 0) {
            badge = Badge.CONTRIBUTEUR;
        } else {
            badge = Badge.NOUVEAU;
        }
        return new Reputation(rounded, ratingsCount, sessionsHelped, badge);
    }

    public static Reputation none() {
        return of(null, 0, 0);
    }
}
