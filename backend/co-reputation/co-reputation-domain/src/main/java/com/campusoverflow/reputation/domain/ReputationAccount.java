package com.campusoverflow.reputation.domain;

// 聚合根 ReputationAccount
public class ReputationAccount {

    private final ReputationAccountId id;
    private int score;

    // 构造器
    public ReputationAccount(ReputationAccountId id, int score) {
        this.id = id;
        this.score = score;
    }

    // 领域行为（业务方法，不要放setter）
    public void addScore(int delta) {
        this.score += delta;
    }

    // getter
    public ReputationAccountId getId() {
        return id;
    }

    public int getScore() {
        return score;
    }
}
