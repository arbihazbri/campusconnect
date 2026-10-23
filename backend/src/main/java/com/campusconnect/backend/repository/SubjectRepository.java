package com.campusconnect.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusconnect.backend.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByName(String name);

    boolean existsByName(String name);
}