package com.campusconnect.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.backend.entity.HelpOffer;

public interface HelpOfferRepository extends JpaRepository<HelpOffer, Long> {

    boolean existsByHelperIdAndRequestId(Long helperId, Long requestId);

    @EntityGraph(attributePaths = { "helper", "request" })
    List<HelpOffer> findByRequestIdOrderByCreatedAtAscIdAsc(Long requestId);

    List<HelpOffer> findByHelperId(Long helperId);

    List<HelpOffer> findByRequestId(Long requestId);

    /**
     * Renvoie seulement l'identifiant de la demande liée à la proposition (une valeur simple,
     * qui ne charge pas la proposition en mémoire).
     */
    @Query("select o.request.id from HelpOffer o where o.id = :offerId")
    Optional<Long> findRequestIdById(@Param("offerId") Long offerId);
}