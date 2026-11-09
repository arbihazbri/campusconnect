package com.campusconnect.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.RequestStatus;

public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long> {

    @EntityGraph(attributePaths = { "author", "subject" })
    Optional<HelpRequest> findWithDetailsById(Long id);

    /**
     * Charge la demande en la verrouillant (SELECT ... FOR UPDATE) jusqu'à la fin de la transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from HelpRequest r where r.id = :id")
    Optional<HelpRequest> findByIdForUpdate(@Param("id") Long id);

    List<HelpRequest> findByAuthorId(Long authorId);

    /** Demandes publiées par l'auteur (masquées comprises), les plus récentes d'abord. */
    @EntityGraph(attributePaths = { "subject" })
    List<HelpRequest> findByAuthorIdOrderByCreatedAtDescIdDesc(Long authorId);

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

    /**
     * Fonctionnalité originale : demandes ouvertes dans les compétences de l'étudiant, hors les siennes
     * et celles où il a déjà proposé son aide. Même niveau d'abord, puis les plus récentes.
     */
    @EntityGraph(attributePaths = { "author", "subject" })
    @Query("""
            select r from HelpRequest r
            where r.hidden = false
              and r.status = com.campusconnect.backend.entity.RequestStatus.OUVERTE
              and r.author.id <> :userId
              and r.subject.id in :subjectIds
              and not exists (select o.id from HelpOffer o where o.request = r and o.helper.id = :userId)
            order by case when r.level = :level then 0 else 1 end, r.createdAt desc, r.id desc
            """)
    List<HelpRequest> findRecommendations(
            @Param("userId") Long userId,
            @Param("subjectIds") Collection<Long> subjectIds,
            @Param("level") Level level,
            Pageable pageable);
}
