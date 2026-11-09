package com.campusconnect.backend.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.Reputation;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.RatingRepository;

/** Calcule la réputation (note moyenne, aides terminées, badge) de plusieurs étudiants en deux requêtes. */
@Service
public class ReputationService {

    private final RatingRepository ratingRepository;
    private final HelpOfferRepository helpOfferRepository;

    public ReputationService(RatingRepository ratingRepository, HelpOfferRepository helpOfferRepository) {
        this.ratingRepository = ratingRepository;
        this.helpOfferRepository = helpOfferRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, Reputation> of(Collection<Long> userIds) {
        Map<Long, Reputation> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        Map<Long, Double> averages = new HashMap<>();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : ratingRepository.ratingStatsByRated(userIds)) {
            averages.put((Long) row[0], ((Number) row[1]).doubleValue());
            counts.put((Long) row[0], ((Number) row[2]).longValue());
        }
        Map<Long, Long> helped = new HashMap<>();
        for (Object[] row : helpOfferRepository.countHelpedSessionsByHelper(userIds)) {
            helped.put((Long) row[0], ((Number) row[1]).longValue());
        }
        for (Long id : userIds) {
            result.put(id, Reputation.of(averages.get(id), counts.getOrDefault(id, 0L), helped.getOrDefault(id, 0L)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Reputation of(Long userId) {
        return of(List.of(userId)).get(userId);
    }
}
