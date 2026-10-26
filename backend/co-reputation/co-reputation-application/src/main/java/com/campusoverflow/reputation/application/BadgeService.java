package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 徽章授予（幂等：已拥有则忽略）。
 */
@Service
public class BadgeService {

    private final BadgeRepository badges;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public BadgeService(BadgeRepository badges, IntegrationEventPublisher events, Clock clock) {
        this.badges = badges;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void grant(long userId, BadgeType type, String sourceRefId) {
        // 幂等判断：用户已经持有该徽章，直接返回
        if (badges.existsByUserIdAndType(userId, type)) {
            return;
        }
        Instant now = Instant.now(clock);
        BadgeAward award = BadgeAward.create(userId, type, now);
        badges.save(award);

        // 5个参数，对齐 BadgeAwardedEvent record，解决参数数量报错
        BadgeAwardedEvent awardedEvent = new BadgeAwardedEvent(
                EventIds.next(),
                now,
                userId,
                type,
                sourceRefId
        );
        events.publish(awardedEvent);
    }
}
