package com.campusconnect.backend.dto;

import com.campusconnect.backend.entity.Subject;

public record SubjectResponse(Long id, String name) {

    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName());
    }
}