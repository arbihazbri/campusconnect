package com.campusconnect.backend.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.backend.entity.Rating;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByRequestIdAndRaterId(Long requestId, Long raterId);

    @EntityGraph(attributePaths = { "rater", "rated", "request" })
    List<Rating> findByRequestIdOrderByCreatedAtAscIdAsc(Long requestId);

    List<Rating> findByRequestId(Long requestId);

    List<Rating> findByRaterIdOrRatedId(Long raterId, Long ratedId);

    /** Identifiants des demandes que l'utilisateur a déjà évaluées. */
    @Query("select r.request.id from Rating r where r.rater.id = :userId")
    List<Long> findRatedRequestIdsByRater(@Param("userId") Long userId);

    /** Évaluations reçues par l'utilisateur, avec la demande associée. */
    @EntityGraph(attributePaths = { "request" })
    List<Rating> findByRatedId(Long ratedId);

    /** [identifiant évalué, moyenne, nombre] pour chaque étudiant de la liste ayant reçu au moins une note. */
    @Query("""
            select r.rated.id, avg(r.score), count(r) from Rating r
            where r.rated.id in :userIds
            group by r.rated.id
            """)
    List<Object[]> ratingStatsByRated(@Param("userIds") Collection<Long> userIds);
}
