package com.campusconnect.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.campusconnect.backend.entity.HelpOffer;

public interface HelpOfferRepository extends JpaRepository<HelpOffer, Long> {

    boolean existsByHelperIdAndRequestId(Long helperId, Long requestId);

    @EntityGraph(attributePaths = { "helper", "request" })
    List<HelpOffer> findByRequestIdOrderByCreatedAtAscIdAsc(Long requestId);

    List<HelpOffer> findByHelperId(Long helperId);

    List<HelpOffer> findByRequestId(Long requestId);
}