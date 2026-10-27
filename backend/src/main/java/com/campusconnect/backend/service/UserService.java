package com.campusconnect.backend.service;

import org.springframework.stereotype.Service;

import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.exception.ResourceNotFoundException;
import com.campusconnect.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}