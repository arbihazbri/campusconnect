package com.campusconnect.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.campusconnect.backend.entity.HelpRequest;

public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long> {

    @EntityGraph(attributePaths = { "author", "subject" })
    Optional<HelpRequest> findWithDetailsById(Long id);

    List<HelpRequest> findByAuthorId(Long authorId);
}