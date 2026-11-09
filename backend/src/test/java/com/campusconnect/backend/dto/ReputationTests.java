package com.campusconnect.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.campusconnect.backend.dto.Reputation.Badge;

class ReputationTests {

    @Test
    void badgeThresholds() {
        assertThat(Reputation.of(null, 0, 0).badge()).isEqualTo(Badge.NOUVEAU);
        assertThat(Reputation.of(2.0, 3, 1).badge()).isEqualTo(Badge.CONTRIBUTEUR);
        assertThat(Reputation.of(4.6, 9, 4).badge()).isEqualTo(Badge.CONTRIBUTEUR);
        assertThat(Reputation.of(3.9, 9, 8).badge()).isEqualTo(Badge.CONTRIBUTEUR);
        assertThat(Reputation.of(4.0, 5, 5).badge()).isEqualTo(Badge.MENTOR);
    }

    @Test
    void averageIsRoundedToOneDecimal() {
        assertThat(Reputation.of(4.666, 3, 1).averageRating()).isEqualTo(4.7);
        assertThat(Reputation.of(3.94, 3, 5).averageRating()).isEqualTo(3.9);
        assertThat(Reputation.of(3.95, 3, 5).badge()).isEqualTo(Badge.MENTOR);
    }
}
