package com.campusoverflow.identity.application;

import com.campusoverflow.identity.domain.CourseApplication;
import com.campusoverflow.identity.domain.CourseApplicationRepository;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CourseApplicationService {

    private final CourseApplicationRepository courseApplicationRepository;
    private final IdentityApi identityApi;
    private final Clock clock;
    private final AuditTrail auditTrail;

    public CourseApplicationService(CourseApplicationRepository courseApplicationRepository,
                                    IdentityApi identityApi,
                                    Clock clock,
                                    AuditTrail auditTrail) {
        this.courseApplicationRepository = courseApplicationRepository;
        this.identityApi = identityApi;
        this.clock = clock;
        this.auditTrail = auditTrail;
    }

    /**
     * 审批通过选课申请
     * @param operator 操作者
     * @param applicationId 申请单id
     */
    @Transactional
    public void approve(Actor operator, Long applicationId) {
        CourseApplication application = courseApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("ID‑4003", "选课申请不存在"));

        boolean canManage = identityApi.canManageCourse(operator.userId(), application.getCourseId());
        if (!canManage) {
            throw new ForbiddenException("ID‑3003", "只有课程管理者可以审批选课申请");
        }

        application.approve(clock.instant());
        courseApplicationRepository.save(application);

        auditTrail.record(
                "COURSE_APPLICATION_APPROVE",
                operator.userId(),
                "CourseApplication",
                String.valueOf(applicationId),
                null
        );
    }

    /**
     * 驳回选课申请
     * @param operator 操作者
     * @param applicationId 申请单id
     * @param rejectReason 驳回理由
     */
    @Transactional
    public void reject(Actor operator, Long applicationId, String rejectReason) {
        CourseApplication application = courseApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("ID‑4003", "选课申请不存在"));

        boolean canManage = identityApi.canManageCourse(operator.userId(), application.getCourseId());
        if (!canManage) {
            throw new ForbiddenException("ID‑3003", "只有课程管理者可以审批选课申请");
        }

        application.reject(rejectReason, clock.instant());
        courseApplicationRepository.save(application);

        auditTrail.record(
                "COURSE_APPLICATION_REJECT",
                operator.userId(),
                "CourseApplication",
                String.valueOf(applicationId),
                "rejectReason:" + rejectReason
        );
    }
}
