package com.campusconnect.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.RequestStatus;

public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long> {

    @EntityGraph(attributePaths = { "author", "subject" })
    Optional<HelpRequest> findWithDetailsById(Long id);

    List<HelpRequest> findByAuthorId(Long authorId);

    @EntityGraph(attributePaths = { "author", "subject" })
    @Query("""
            select r from HelpRequest r
            where r.hidden = false
              and (:subjectId is null or r.subject.id = :subjectId)
              and (:level is null or r.level = :level)
              and (:status is null or r.status = :status)
            """)
    Page<HelpRequest> search(
            @Param("subjectId") Long subjectId,
            @Param("level") Level level,
            @Param("status") RequestStatus status,
            Pageable pageable);
}