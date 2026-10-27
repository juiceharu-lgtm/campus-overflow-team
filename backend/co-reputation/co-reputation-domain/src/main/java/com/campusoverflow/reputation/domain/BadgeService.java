package com.campusoverflow.reputation.domain;

import java.time.Instant;

public interface BadgeService {
    void evaluateAndAward(Long userId, BadgeAward.Trigger trigger, Instant now);
}
