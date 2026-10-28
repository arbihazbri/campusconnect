package com.campusconnect.backend.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.OfferInput;
import com.campusconnect.backend.dto.OfferResponse;
import com.campusconnect.backend.entity.HelpOffer;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.RequestStatus;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.ConflictException;
import com.campusconnect.backend.exception.ForbiddenOperationException;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.HelpOfferRepository;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.UserRepository;

@Service
public class OfferService {

    private final HelpOfferRepository helpOfferRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final UserRepository userRepository;

    public OfferService(
            HelpOfferRepository helpOfferRepository,
            HelpRequestRepository helpRequestRepository,
            UserRepository userRepository) {
        this.helpOfferRepository = helpOfferRepository;
        this.helpRequestRepository = helpRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OfferResponse create(Long requestId, Long helperId, OfferInput input) {
        HelpRequest request = findVisibleRequest(requestId);

        if (request.getAuthor().getId().equals(helperId)) {
            throw new ForbiddenOperationException(
                    "Vous ne pouvez pas proposer votre aide sur votre propre demande");
        }
        if (request.getStatus() != RequestStatus.OUVERTE) {
            throw new ConflictException("Cette demande n'accepte plus de propositions");
        }
        if (helpOfferRepository.existsByHelperIdAndRequestId(helperId, requestId)) {
            throw new ConflictException("Vous avez déjà proposé votre aide sur cette demande");
        }

        User helper = userRepository.findById(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        HelpOffer offer = new HelpOffer();
        offer.setMessage(input.message().trim());
        offer.setHelper(helper);
        offer.setRequest(request);

        HelpOffer saved;
        try {
            saved = helpOfferRepository.saveAndFlush(offer);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Vous avez déjà proposé votre aide sur cette demande");
        }
        return OfferResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listForRequest(Long requestId, Long currentUserId) {
        HelpRequest request = findVisibleRequest(requestId);

        if (!request.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("Seul l'auteur peut consulter les propositions");
        }

        return helpOfferRepository.findByRequestIdOrderByCreatedAtAscIdAsc(requestId).stream()
                .map(OfferResponse::from)
                .toList();
    }

    private HelpRequest findVisibleRequest(Long requestId) {
        return helpRequestRepository.findWithDetailsById(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
    }
}