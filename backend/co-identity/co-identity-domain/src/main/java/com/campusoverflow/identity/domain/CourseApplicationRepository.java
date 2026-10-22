package com.campusoverflow.identity.domain;

import java.util.Optional;

public interface CourseApplicationRepository {
    Optional<CourseApplication> findById(Long id);
    void save(CourseApplication application);
}
