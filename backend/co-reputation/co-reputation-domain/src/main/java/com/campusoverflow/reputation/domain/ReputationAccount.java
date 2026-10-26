package com.campusoverflow.reputation.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;

/**
 * 声誉账户。不变量：reputation 恒等于该用户所有账本流水 delta 之和（由对账任务 FF‑8 验证）。
 * 悬赏积分 = 可用 + 冻结，冻结部分在悬赏结算或到期时释放。
 */
public class ReputationAccount {

    private final long userId;
    private int reputation;
    private int availablePoints;
    private int frozenPoints;
    private final long version;

    private ReputationAccount(long userId, int reputation, int availablePoints, int frozenPoints, long version) {
        this.userId = userId;
        this.reputation = reputation;
        this.availablePoints = availablePoints;
        this.frozenPoints = frozenPoints;
        this.version = version;
    }

    /**
     * 开立新账户：声誉默认0；initialPoints为悬赏可用积分（教师1000 / 学生0）
     */
    public static ReputationAccount open(long userId, int initialPoints) {
        requirePositive(initialPoints);
        // 新账户：声誉0，可用悬赏积分=initialPoints，冻结0，版本0
        return new ReputationAccount(userId, 0, initialPoints, 0, 0L);
    }

    public static ReputationAccount reconstitute(long userId, int reputation, int availablePoints, int frozenPoints,
                                                 long version) {
        return new ReputationAccount(userId, reputation, availablePoints, frozenPoints, version);
    }

    /**
     * 只允许通过账本流水改变声誉（由应用服务保证先写流水）。
     * FF‑8：聚合内部**不截断负分**，允许reputation变成负数；业务拦截放在上层Rulebook。
     */
    public void applyReputation(int delta) {
        this.reputation += delta;
    }

    /**
     * 冻结悬赏积分：从可用扣到冻结
     */
    public void freezePoints(int points) {
        requirePositive(points);
        if (availablePoints < points) {
            throw new BusinessRuleException("REP‑1002", "可用悬赏积分不足");
        }
        availablePoints -= points;
        frozenPoints += points;
    }

    /**
     * 悬赏成功发放：冻结积分转出给获奖者（以声誉形式），发起人账户中消失。
     */
    public void spendFrozen(int points) {
        requirePositive(points);
        ensureFrozen(points);
        frozenPoints -= points;
    }

    /**
     * 悬赏到期：冻结积分退回可用。
     */
    public void refundFrozen(int points) {
        requirePositive(points);
        ensureFrozen(points);
        frozenPoints -= points;
        availablePoints += points;
    }

    /**
     * 校验：当前冻结余额足够扣减points
     */
    private void ensureFrozen(int points) {
        if (frozenPoints < points) {
            throw new BusinessRuleException("REP‑1006", "冻结悬赏积分不足");
        }
    }

    /**
     * 积分参数必须大于0
     */
    private static void requirePositive(int points) {
        if (points <= 0) {
            throw new BusinessRuleException("REP‑1006", "积分必须为正数");
        }
    }

    // getter
    public long userId() { return userId; }
    public int reputation() { return reputation; }
    public int availablePoints() { return availablePoints; }
    public int frozenPoints() { return frozenPoints; }
    public long version() { return version; }
}
