package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationLedgerService;
import com.campusoverflow.reputation.infrastructure.persistence.ReputationAccountPO;
import com.campusoverflow.reputation.infrastructure.persistence.ReputationAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class ReputationLedgerServiceImpl implements ReputationLedgerService {

    private final ReputationAccountRepository accountRepo;

    public ReputationLedgerServiceImpl(ReputationAccountRepository accountRepo) {
        this.accountRepo = accountRepo;
    }

    @Override
    public boolean existsAccount(Long userId) {
        return accountRepo.existsByUserId(userId);
    }

    @Override
    @Transactional
    public ReputationAccount createAccount(Long userId, int initScore, Instant now) {
        ReputationAccount domain = ReputationAccount.create(userId, initScore, now);
        ReputationAccountPO po = ReputationAccountPO.fromDomain(domain);
        ReputationAccountPO saved = accountRepo.save(po);
        return saved.toDomain();
    }

    @Override
    @Transactional
    public void addScore(Long userId, int delta, Instant now) {
        ReputationAccountPO po = accountRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("声望账户不存在 userId:" + userId));
        ReputationAccount domain = po.toDomain();
        domain.addScore(delta, now);
        accountRepo.save(ReputationAccountPO.fromDomain(domain));
    }
}
