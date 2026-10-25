package com.campusoverflow.reputation.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;
import java.time.Instant;
import java.util.UUID;

public class ReputationScore {
    private final UUID id;
    private final UUID userId;
    private int score;
    private Instant lastUpdateAt;

    private ReputationScore(UUID id, UUID userId, int initScore) {
        this.id = id;
        this.userId = userId;
        this.score = initScore;
        this.lastUpdateAt = Instant.now();
    }

    public static ReputationScore create(UUID userId) {
        // 初始声誉分值
        int initialScore = 10;
        return new ReputationScore(UUID.randomUUID(), userId, initialScore);
    }

    public void adjustScore(int delta) {
        // 声誉最低不能小于0
        if (this.score + delta < 0) {
            throw new BusinessRuleException("REP-1001", "声誉分值不能小于0");
        }
        this.score += delta;
        this.lastUpdateAt = Instant.now();
    }

    // getter
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public int getScore() { return score; }
    public Instant getLastUpdateAt() { return lastUpdateAt; }
}
