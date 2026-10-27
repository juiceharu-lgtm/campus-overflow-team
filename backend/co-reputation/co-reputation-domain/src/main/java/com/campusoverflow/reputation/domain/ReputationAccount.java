package com.campusoverflow.reputation.domain;

import java.time.Instant;

public class ReputationAccount {

    private Long id;
    private Long userId;
    private int totalScore;
    private Instant createdAt;
    private Instant updatedAt;

    protected ReputationAccount() {
    }

    private ReputationAccount(Long userId, int initScore, Instant now) {
        this.userId = userId;
        this.totalScore = initScore;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ReputationAccount create(Long userId, int initScore, Instant now) {
        return new ReputationAccount(userId, initScore, now);
    }

    public void addScore(int delta, Instant now) {
        this.totalScore += delta;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
