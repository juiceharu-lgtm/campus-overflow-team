package com.campusoverflow.reputation.infrastructure.persistence;

import com.campusoverflow.reputation.domain.entity.BadgeAward;
import com.campusoverflow.reputation.domain.entity.ReputationAccount;
import com.campusoverflow.reputation.domain.BadgeAwardRepository;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReputationPersistenceAdapter implements ReputationAccountRepository, BadgeAwardRepository {

    private final AccountJpaRepository accountJpaRepository;
    private final BadgeJpaRepository badgeJpaRepository;

    // ========= ReputationAccount 适配 =========
    @Override
    public Optional<ReputationAccount> findByUserId(Long userId) {
        return accountJpaRepository.findByUserId(userId)
                .map(this::toDomain);
    }

    @Override
    public ReputationAccount save(ReputationAccount domain) {
        AccountJpaEntity po = toPo(domain);
        AccountJpaEntity saved = accountJpaRepository.save(po);
        return toDomain(saved);
    }

    // ========= BadgeAward 适配 =========
    @Override
    public BadgeAward save(BadgeAward domain) {
        BadgeJpaEntity po = toPo(domain);
        BadgeJpaEntity saved = badgeJpaRepository.save(po);
        return toDomain(saved);
    }

    // PO <-> Domain 转换方法，你按自己字段补全
    private ReputationAccount toDomain(AccountJpaEntity po) {
        return ReputationAccount.rebuild(po.getId(), po.getUserId(), po.getScore());
    }
    private AccountJpaEntity toPo(ReputationAccount domain) {
        AccountJpaEntity po = new AccountJpaEntity();
        po.setId(domain.getId());
        po.setUserId(domain.getUserId());
        po.setScore(domain.getScore());
        return po;
    }

    private BadgeAward toDomain(BadgeJpaEntity po) {
        return BadgeAward.rebuild(po.getId(), po.getUserId(), po.getBadgeCode(), po.getAwardTime());
    }
    private BadgeJpaEntity toPo(BadgeAward domain) {
        BadgeJpaEntity po = new BadgeJpaEntity();
        po.setId(domain.getId());
        po.setUserId(domain.getUserId());
        po.setBadgeCode(domain.getBadgeCode());
        po.setAwardTime(domain.getAwardTime());
        return po;
    }
}
