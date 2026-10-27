package com.campusconnect.backend.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.backend.dto.UpdateProfileRequest;
import com.campusconnect.backend.dto.UserResponse;
import com.campusconnect.backend.entity.Subject;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.BusinessValidationException;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.SubjectRepository;
import com.campusconnect.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    public UserService(UserRepository userRepository, SubjectRepository subjectRepository) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        Set<Long> skillIds = request.skillIds() != null ? request.skillIds() : Set.of();
        List<Subject> subjects = subjectRepository.findAllById(skillIds);
        if (subjects.size() != skillIds.size()) {
            throw new BusinessValidationException("skillIds", "Une ou plusieurs matières sont inconnues");
        }

        user.setFullName(request.fullName().trim());
        user.setSpecialty(blankToNull(request.specialty()));
        user.setLevel(request.level());
        user.getSkills().clear();
        user.getSkills().addAll(subjects);

        return UserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}