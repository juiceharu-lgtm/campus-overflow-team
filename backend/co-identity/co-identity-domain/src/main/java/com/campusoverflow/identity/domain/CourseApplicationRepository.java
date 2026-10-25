package com.campusoverflow.identity.domain;

import java.util.Optional;

public interface CourseApplicationRepository {
    Optional<CourseApplication> findById(Long applicationId);
    void save(CourseApplication application);
}
