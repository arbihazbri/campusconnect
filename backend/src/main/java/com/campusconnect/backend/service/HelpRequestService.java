package com.campusconnect.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.HelpRequestInput;
import com.campusconnect.backend.dto.HelpRequestResponse;
import com.campusconnect.backend.entity.HelpRequest;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.BusinessValidationException;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.HelpRequestRepository;
import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.repository.UserRepository;

@Service
public class HelpRequestService {

    private final HelpRequestRepository helpRequestRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    public HelpRequestService(
            HelpRequestRepository helpRequestRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository) {
        this.helpRequestRepository = helpRequestRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public HelpRequestResponse create(Long authorId, HelpRequestInput input) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Subject subject = subjectRepository.findById(input.subjectId())
                .orElseThrow(() -> new BusinessValidationException("subjectId", "Matière inconnue"));

        HelpRequest request = new HelpRequest();
        request.setTitle(input.title().trim());
        request.setDescription(input.description().trim());
        request.setAvailability(input.availability().trim());
        request.setLevel(input.level());
        request.setAuthor(author);
        request.setSubject(subject);

        helpRequestRepository.save(request);
        return HelpRequestResponse.from(request, authorId);
    }

    @Transactional(readOnly = true)
    public HelpRequestResponse getById(Long requestId, Long currentUserId) {
        HelpRequest request = helpRequestRepository.findWithDetailsById(requestId)
                .filter(found -> !found.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable"));
        return HelpRequestResponse.from(request, currentUserId);
    }
}