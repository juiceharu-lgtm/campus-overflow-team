package com.campusoverflow.reputation.domain;

import java.time.Instant;

public class BadgeAward {

    // 内部枚举，对应 BadgeAward.Trigger
    public enum Trigger {
        ANSWER,
        ACCEPT,
        LIKE,
        CHECK_IN
    }

    private Long id;
    private Long userId;
    private String badgeCode;
    private Instant awardTime;

    // 领域构造方法，发放徽章
    public static BadgeAward award(Long userId, String badgeCode, Instant awardTime) {
        BadgeAward award = new BadgeAward();
        award.userId = userId;
        award.badgeCode = badgeCode;
        award.awardTime = awardTime;
        return award;
    }

    // getter
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getBadgeCode() {
        return badgeCode;
    }

    public Instant getAwardTime() {
        return awardTime;
    }
}
