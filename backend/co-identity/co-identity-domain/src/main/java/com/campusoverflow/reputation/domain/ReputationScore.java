package com.campusoverflow.reputation.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;
import java.util.UUID;

public class ReputationScore {

    private final UUID userId;
    private int score;

    private ReputationScore(UUID userId, int initialScore) {
        this.userId = userId;
        this.score = initialScore;
    }

    /**
     * 工厂方法：创建用户声誉记录，初始分数固定10
     */
    public static ReputationScore create(UUID userId) {
        return new ReputationScore(userId, 10);
    }

    /**
     * 领域方法：调整声誉分值
     * 【DDD领域不变量】不允许声誉分数小于0；
     * 若本次变更会产生负数，直接抛出异常拒绝本次操作；
     * 不做分数钳位置0，保障FF‑8对账：账户余额 = 流水总和
     */
    public void adjustScore(int delta) {
        int newScore = this.score + delta;
        if (newScore < 0) {
            throw new BusinessRuleException("REP-1001", "声誉分值不能小于0");
        }
        this.score = newScore;
    }

    // reconstitute：仓储重建聚合，不执行业务校验
    public static ReputationScore reconstitute(UUID userId, int score) {
        return new ReputationScore(userId, score);
    }

    // getter
    public UUID getUserId() {
        return userId;
    }

    public int getScore() {
        return score;
    }
}
