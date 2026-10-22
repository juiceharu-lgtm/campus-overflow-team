package com.campusoverflow.identity.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.security.Actor;
import java.time.Instant;
import java.util.Objects;

public class CourseApplication {
    private Long id;
    private final Long applicantUserId;
    private CourseApplicationStatus status;
    private Actor reviewer;
    private String reviewComment;
    private final Instant applyAt;
    private final long version;

    private CourseApplication(Long id, Long applicantUserId, long version) {
        this.id = id;
        this.applicantUserId = Objects.requireNonNull(applicantUserId);
        this.status = CourseApplicationStatus.PENDING;
        this.applyAt = Instant.now();
        this.version = version;
    }

    /**
     * 创建选课申请，新建默认状态：PENDING
     */
    public static CourseApplication create(Long applicantUserId) {
        return new CourseApplication(null, applicantUserId, 0);
    }

    /**
     * 仓储重建聚合（reconstitute 写法和 User 保持一致，持久化专用）
     */
    public static CourseApplication reconstitute(Long id, Long applicantUserId, CourseApplicationStatus status,
                                                 Actor reviewer, String reviewComment, Instant applyAt, long version) {
        CourseApplication app = new CourseApplication(id, applicantUserId, version);
        app.status = status;
        app.reviewer = reviewer;
        app.reviewComment = reviewComment;
        return app;
    }

    /**
     * 审批业务方法
     * 不变式：只有 PENDING 才允许审批，不能重复审批
     */
    public void approve(Actor reviewer, CourseApplicationStatus newStatus, String comment) {
        if (!CourseApplicationStatus.PENDING.equals(this.status)) {
            throw new BusinessRuleException("ID-4001", "仅待审批状态的选课申请允许审批，禁止重复审批");
        }
        if (!CourseApplicationStatus.APPROVED.equals(newStatus)
                && !CourseApplicationStatus.REJECTED.equals(newStatus)) {
            throw new BusinessRuleException("ID-4002", "审批结果只能选择 APPROVED 或者 REJECTED");
        }
        this.status = newStatus;
        this.reviewer = Objects.requireNonNull(reviewer);
        this.reviewComment = comment;
    }

    /** 分配主键，和User.assignId 风格对齐 */
    public void assignId(long id) {
        if (this.id != null) {
            throw new IllegalStateException("ID 已分配");
        }
        this.id = id;
    }

    // 访问器：和项目统一风格，不带get前缀
    public Long id() { return id; }
    public Long applicantUserId() { return applicantUserId; }
    public CourseApplicationStatus status() { return status; }
    public Actor reviewer() { return reviewer; }
    public String reviewComment() { return reviewComment; }
    public Instant applyAt() { return applyAt; }
    public long version() { return version; }
}
