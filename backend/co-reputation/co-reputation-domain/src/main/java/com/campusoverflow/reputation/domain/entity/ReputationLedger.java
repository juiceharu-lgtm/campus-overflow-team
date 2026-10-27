package com.campusoverflow.reputation.domain.entity;

import java.time.LocalDateTime;

public class ReputationLedger {
    private final ReputationLedgerId id;
    private final Long userId;
    private final Integer scoreChange;
    private final String reason;
    private final LocalDateTime createTime;

    // 全参构造
    public ReputationLedger(ReputationLedgerId id, Long userId, Integer scoreChange, String reason, LocalDateTime createTime) {
        this.id = id;
        this.userId = userId;
        this.scoreChange = scoreChange;
        this.reason = reason;
        this.createTime = createTime;
    }

    // getter，不要setter！
    public ReputationLedgerId getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getScoreChange() {
        return scoreChange;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }
}
