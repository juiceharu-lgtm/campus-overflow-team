package com.campusoverflow.reputation.domain;

import java.time.Instant;

public class BadgeAward {

    public enum Trigger {
        REGISTER,
        ANSWER_SUBMIT,
        ANSWER_ACCEPTED,
        VOTE,
        ANSWER_ENDORSED
    }

    private Long id;
    private Long userId;
    private String badgeCode;
    private Trigger trigger;
    private Instant awardedAt;

    protected BadgeAward() {
    }

    public BadgeAward(Long userId, String badgeCode, Trigger trigger, Instant now) {
        this.userId = userId;
        this.badgeCode = badgeCode;
        this.trigger = trigger;
        this.awardedAt = now;
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

    public String getBadgeCode() {
        return badgeCode;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public Instant getAwardedAt() {
        return awardedAt;
    }
}
