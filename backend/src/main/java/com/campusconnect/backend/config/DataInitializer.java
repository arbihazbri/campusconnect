package com.campusconnect.backend.config;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.repository.UserRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final List<String> DEFAULT_SUBJECTS = List.of(
            "Mathématiques",
            "Algorithmique",
            "Programmation Java",
            "Bases de données",
            "Développement web",
            "Réseaux",
            "Systèmes d'exploitation",
            "Génie logiciel",
            "Intelligence artificielle",
            "Anglais technique");

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(
            SubjectRepository subjectRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedSubjects();
        seedAdmin();
    }

    private void seedSubjects() {
        int created = 0;
        for (String name : DEFAULT_SUBJECTS) {
            if (!subjectRepository.existsByName(name)) {
                Subject subject = new Subject();
                subject.setName(name);
                subjectRepository.save(subject);
                created++;
            }
        }
        log.info("Matières initiales : {} ajoutée(s), {} au total", created, subjectRepository.count());
    }

    private void seedAdmin() {
        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || adminPassword.isBlank()) {
            log.warn("Compte administrateur non créé : ADMIN_EMAIL et ADMIN_PASSWORD ne sont pas définis");
            return;
        }
        if (userRepository.existsByEmail(email)) {
            log.info("Compte administrateur déjà présent");
            return;
        }
        User admin = new User();
        admin.setEmail(email);
        admin.setFullName("Administrateur");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Compte administrateur créé");
    }
}