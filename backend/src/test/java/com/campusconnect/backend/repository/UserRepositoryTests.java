package com.campusconnect.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.entity.User;

@SpringBootTest
@Transactional
class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    private User newUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash-de-test");
        user.setFullName("Etudiant Test");
        user.setRole(Role.STUDENT);
        return user;
    }

    @Test
    void saveUserWithSkillsThenFindByEmail() {
        Subject subject = new Subject();
        subject.setName("Matiere de test");
        subjectRepository.save(subject);

        User user = newUser("test@campusconnect.local");
        user.getSkills().add(subject);
        userRepository.saveAndFlush(user);

        User found = userRepository.findByEmail("test@campusconnect.local").orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getSkills()).hasSize(1);
    }

    @Test
    void duplicateEmailIsRejectedByTheDatabase() {
        userRepository.saveAndFlush(newUser("doublon@campusconnect.local"));

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(newUser("doublon@campusconnect.local")));
    }
}