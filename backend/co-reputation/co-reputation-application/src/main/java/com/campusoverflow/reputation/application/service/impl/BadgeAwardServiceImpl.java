package com.campusoverflow.reputation.application.service.impl;

import com.campusoverflow.reputation.application.service.BadgeService;
import com.campusoverflow.reputation.domain.entity.BadgeAward;
import com.campusoverflow.reputation.domain.repository.BadgeAwardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BadgeAwardServiceImpl implements BadgeService {

    private final BadgeAwardRepository badgeAwardRepository;

    @Override
    public void awardBadge(Long userId, String badgeCode) {
        BadgeAward award = new BadgeAward();
        award.setUserId(userId);
        award.setBadgeCode(badgeCode);
        badgeAwardRepository.save(award);
    }
}
