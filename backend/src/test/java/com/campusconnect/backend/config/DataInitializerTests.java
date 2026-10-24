package com.campusconnect.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.campusconnect.backend.repository.SubjectRepository;

@SpringBootTest
class DataInitializerTests {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void defaultSubjectsAreSeededAtStartup() {
        assertThat(subjectRepository.existsByName("Mathématiques")).isTrue();
        assertThat(subjectRepository.count()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void passwordEncoderHashesWithBCrypt() {
        String hash = passwordEncoder.encode("MotDePasse123");

        assertThat(hash).isNotEqualTo("MotDePasse123");
        assertThat(hash).startsWith("$2");
        assertThat(passwordEncoder.matches("MotDePasse123", hash)).isTrue();
        assertThat(passwordEncoder.matches("autre", hash)).isFalse();
    }
}