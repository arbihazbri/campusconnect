package com.campusconnect.backend.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.HelpRequestResponse;
import com.campusconnect.backend.dto.RecommendationsResponse;
import com.campusconnect.backend.dto.RecommendationsResponse.Recommendation;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.UserRepository;

/**
 * Fonctionnalité originale : propose à un étudiant les demandes ouvertes qu'il est le mieux placé pour aider.
 * Critères : la matière fait partie de ses compétences, la demande n'est pas la sienne, n'est pas masquée
 * et il n'a pas déjà proposé son aide. Les demandes de son niveau passent en premier, puis les plus récentes.
 */
@Service
public class RecommendationService {

    public static final int MAX_ITEMS = 20;

    private final UserRepository userRepository;
    private final HelpRequestRepository helpRequestRepository;

    public RecommendationService(UserRepository userRepository, HelpRequestRepository helpRequestRepository) {
        this.userRepository = userRepository;
        this.helpRequestRepository = helpRequestRepository;
    }

    @Transactional(readOnly = true)
    public RecommendationsResponse forUser(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Set<Long> subjectIds = user.getSkills().stream().map(Subject::getId).collect(Collectors.toSet());
        if (subjectIds.isEmpty()) {
            return new RecommendationsResponse(false, List.of());
        }
        int size = Math.min(Math.max(limit, 1), MAX_ITEMS);
        List<Recommendation> items = helpRequestRepository
                .findRecommendations(userId, subjectIds, user.getLevel(), PageRequest.of(0, size))
                .stream()
                .map(request -> new Recommendation(
                        HelpRequestResponse.from(request, userId),
                        user.getLevel() != null && user.getLevel() == request.getLevel()))
                .toList();
        return new RecommendationsResponse(true, items);
    }
}
