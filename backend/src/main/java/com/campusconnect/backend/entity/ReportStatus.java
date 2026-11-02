package com.campusconnect.backend.entity;

public enum ReportStatus {
    /** Signalement pas encore examiné par un administrateur. */
    EN_ATTENTE,
    /** L'administrateur a masqué la publication signalée. */
    TRAITE,
    /** L'administrateur a jugé le signalement non fondé. */
    REJETE
}
