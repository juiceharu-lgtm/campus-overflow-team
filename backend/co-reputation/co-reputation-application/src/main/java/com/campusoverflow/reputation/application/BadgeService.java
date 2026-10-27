package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeService;
import com.campusoverflow.reputation.infrastructure.persistence.BadgeAwardPO;
import com.campusoverflow.reputation.infrastructure.persistence.BadgeAwardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class BadgeAwardServiceImpl implements BadgeService {

    private final BadgeAwardRepository badgeRepo;

    public BadgeAwardServiceImpl(BadgeAwardRepository badgeRepo) {
        this.badgeRepo = badgeRepo;
    }

    @Override
    @Transactional
    public void evaluateAndAward(Long userId, BadgeAward.Trigger trigger, Instant now) {
        if (trigger == BadgeAward.Trigger.REGISTER) {
            BadgeAward domain = new BadgeAward(userId, "NEWCOMER", trigger, now);
            BadgeAwardPO po = BadgeAwardPO.fromDomain(domain);
            badgeRepo.save(po);
        }
    }
}
