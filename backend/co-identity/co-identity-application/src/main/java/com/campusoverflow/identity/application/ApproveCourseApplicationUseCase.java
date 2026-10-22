package com.campusoverflow.identity.application;

import com.campusoverflow.identity.domain.CourseApplication;
import com.campusoverflow.identity.domain.CourseApplicationRepository;
import com.campusoverflow.identity.domain.CourseApplicationStatus;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.security.Actor;
import org.springframework.stereotype.Service;

@Service
public class ApproveCourseApplicationUseCase {

    private final CourseApplicationRepository applicationRepository;

    public ApproveCourseApplicationUseCase(CourseApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public void execute(Actor admin, Long applicationId, boolean pass, String comment) {
        CourseApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessRuleException("ID-4003", "选课申请不存在"));

        CourseApplicationStatus targetStatus = pass
                ? CourseApplicationStatus.APPROVED
                : CourseApplicationStatus.REJECTED;

        application.approve(admin, targetStatus, comment);
        applicationRepository.save(application);
    }
}
