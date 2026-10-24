package com.campusconnect.backend.dto;

import com.campusconnect.backend.entity.Level;
import com.campusconnect.backend.entity.Role;
import com.campusconnect.backend.entity.User;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        String specialty,
        Level level) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getSpecialty(),
                user.getLevel());
    }
}