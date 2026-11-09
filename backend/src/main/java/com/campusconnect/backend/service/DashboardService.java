package com.campusconnect.backend.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.AuthorSummary;
import com.campusconnect.backend.dto.DashboardResponse.MyOffer;
import com.campusconnect.backend.dto.DashboardResponse.MyRequest;
import com.campusconnect.backend.dto.DashboardResponse.Session;
import com.campusconnect.backend.dto.DashboardResponse.SessionRole;
import com.campusconnect.backend.dto.DashboardResponse.Stats;
import com.campusconnect.backend.dto.DashboardResponse;
import com.campusconnect.backend.dto.SubjectResponse;
import com.campusconnect.backend.entity.HelpOffer;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.OfferStatus;
import com.campusconnect.backend.entity.Rating;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.RatingRepository;

@Service
public class DashboardService {

    private final HelpRequestRepository helpRequestRepository;
    private final HelpOfferRepository helpOfferRepository;
    private final RatingRepository ratingRepository;
    private final ReputationService reputationService;

    public DashboardService(
            HelpRequestRepository helpRequestRepository,
            HelpOfferRepository helpOfferRepository,
            RatingRepository ratingRepository,
            ReputationService reputationService) {
        this.helpRequestRepository = helpRequestRepository;
        this.helpOfferRepository = helpOfferRepository;
        this.ratingRepository = ratingRepository;
        this.reputationService = reputationService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(Long userId) {
        List<MyRequest> myRequests = myRequests(userId);
        List<MyOffer> myOffers = myOffers(userId);
        List<Session> sessions = sessions(userId);

        List<Rating> received = ratingRepository.findByRatedId(userId);
        Double average = received.isEmpty()
                ? null
                : Math.round(received.stream().mapToInt(Rating::getScore).average().orElse(0) * 10) / 10.0;

        Stats stats = new Stats(
                myRequests.size(),
                myRequests.stream().filter(r -> !r.hidden() && r.status() == RequestStatus.OUVERTE).count(),
                myOffers.size(),
                myOffers.stream().filter(o -> o.status() == OfferStatus.ACCEPTEE).count(),
                sessions.size(),
                received.size(),
                average);

        return new DashboardResponse(stats, myRequests, myOffers, sessions, reputationService.of(userId));
    }

    private List<MyRequest> myRequests(Long userId) {
        Map<Long, long[]> offerCounts = new HashMap<>();
        for (Object[] row : helpOfferRepository.countOffersByRequestForAuthor(userId)) {
            long total = ((Number) row[1]).longValue();
            long pending = row[2] == null ? 0 : ((Number) row[2]).longValue();
            offerCounts.put((Long) row[0], new long[] { total, pending });
        }

        return helpRequestRepository.findByAuthorIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(request -> {
                    long[] counts = offerCounts.getOrDefault(request.getId(), new long[] { 0, 0 });
                    return new MyRequest(
                            request.getId(),
                            request.getTitle(),
                            SubjectResponse.from(request.getSubject()),
                            request.getLevel(),
                            request.getStatus(),
                            request.isHidden(),
                            request.getCreatedAt(),
                            request.getCompletedAt(),
                            counts[0],
                            counts[1]);
                })
                .toList();
    }

    private List<MyOffer> myOffers(Long userId) {
        return helpOfferRepository.findVisibleByHelperId(userId).stream()
                .map(offer -> {
                    HelpRequest request = offer.getRequest();
                    return new MyOffer(
                            offer.getId(),
                            offer.getMessage(),
                            offer.getStatus(),
                            offer.getCreatedAt(),
                            request.getId(),
                            request.getTitle(),
                            request.getStatus(),
                            SubjectResponse.from(request.getSubject()),
                            summary(request.getAuthor()));
                })
                .toList();
    }

    private List<Session> sessions(Long userId) {
        Set<Long> ratedRequestIds = new HashSet<>(ratingRepository.findRatedRequestIdsByRater(userId));

        return helpOfferRepository.findCompletedSessionsOf(userId).stream()
                .map((HelpOffer offer) -> {
                    HelpRequest request = offer.getRequest();
                    boolean isAuthor = request.getAuthor().getId().equals(userId);
                    User partner = isAuthor ? offer.getHelper() : request.getAuthor();
                    return new Session(
                            request.getId(),
                            request.getTitle(),
                            SubjectResponse.from(request.getSubject()),
                            request.getCompletedAt(),
                            isAuthor ? SessionRole.AUTEUR : SessionRole.AIDANT,
                            summary(partner),
                            ratedRequestIds.contains(request.getId()));
                })
                .toList();
    }

    private AuthorSummary summary(User user) {
        return new AuthorSummary(user.getId(), user.getFullName());
    }
}
