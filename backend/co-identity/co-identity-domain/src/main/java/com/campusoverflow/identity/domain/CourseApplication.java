package com.campusoverflow.identity.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;
import java.time.Instant;

public class CourseApplication {
    private final Long id;
    private final Long applicantUserId;
    private final Long courseId;
    private CourseApplicationStatus status;
    private String rejectReason;
    private final Instant applyTime;
    private Instant operateTime;

    private CourseApplication(Long id,
                              Long applicantUserId,
                              Long courseId,
                              CourseApplicationStatus status,
                              String rejectReason,
                              Instant applyTime,
                              Instant operateTime) {
        this.id = id;
        this.applicantUserId = applicantUserId;
        this.courseId = courseId;
        this.status = status;
        this.rejectReason = rejectReason;
        this.applyTime = applyTime;
        this.operateTime = operateTime;
    }

    /**
     * 工厂方法：发起选课申请
     * @param applicantUserId 申请人id
     * @param courseId 课程id
     * @param now 当前时间，由应用层传入，领域禁止直接Instant.now()
     */
    public static CourseApplication create(Long applicantUserId, Long courseId, Instant now) {
        if (applicantUserId == null) {
            throw new BusinessRuleException("ID‑1021", "申请人ID不能为空");
        }
        if (courseId == null) {
            throw new BusinessRuleException("ID‑1022", "课程ID不能为空");
        }
        return new CourseApplication(
                null,
                applicantUserId,
                courseId,
                CourseApplicationStatus.PENDING,
                null,
                now,
                now
        );
    }

    /**
     * 领域方法：审批通过
     * @param now 时间，应用层传入
     */
    public void approve(Instant now) {
        guardCanOperate();
        this.status = CourseApplicationStatus.APPROVED;
        this.rejectReason = null;
        this.operateTime = now;
    }

    /**
     * 领域方法：驳回申请
     * @param reason 驳回理由
     * @param now 时间，应用层传入
     */
    public void reject(String reason, Instant now) {
        guardCanOperate();
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("ID‑1023", "驳回理由不能为空");
        }
        this.status = CourseApplicationStatus.REJECTED;
        this.rejectReason = reason;
        this.operateTime = now;
    }

    /**
     * 领域不变量守卫：只有PENDING待审批状态才允许审批操作
     */
    private void guardCanOperate() {
        if (!CourseApplicationStatus.PENDING.equals(this.status)) {
            throw new BusinessRuleException("ID‑1020", "仅待审批状态的申请可以执行审批操作");
        }
    }

    // ============ getter ============
    public Long getId() {
        return id;
    }

    public Long getApplicantUserId() {
        return applicantUserId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public CourseApplicationStatus getStatus() {
        return status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public Instant getApplyTime() {
        return applyTime;
    }

    public Instant getOperateTime() {
        return operateTime;
    }
}
