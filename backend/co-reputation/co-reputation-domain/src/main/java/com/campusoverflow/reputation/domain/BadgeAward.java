package com.campusoverflow.reputation.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_badge")
public class BadgeAward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeType type;

    @Column(nullable = false)
    private Instant awardedAt;

    protected BadgeAward() {}

    private BadgeAward(long userId, BadgeType type, Instant awardedAt) {
        this.userId = userId;
        this.type = type;
        this.awardedAt = awardedAt;
    }

    public static BadgeAward create(long userId, BadgeType type, Instant awardedAt) {
        return new BadgeAward(userId, type, awardedAt);
    }

    public Long getId() { return id; }
    public long getUserId() { return userId; }
    public BadgeType getType() { return type; }
    public Instant getAwardedAt() { return awardedAt; }
}
