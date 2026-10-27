package com.campusoverflow.reputation.application.service.impl;

import com.campusoverflow.reputation.application.service.ReputationLedgerService;
import com.campusoverflow.reputation.domain.entity.ReputationLedger;
import com.campusoverflow.reputation.domain.repository.ReputationLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReputationLedgerServiceImpl implements ReputationLedgerService {

    private final ReputationLedgerRepository reputationLedgerRepository;

    @Override
    public void addLedger(Long userId, Integer scoreChange, String reason) {
        // 使用全参构造创建领域对象，不再使用setter
        ReputationLedger ledger = new ReputationLedger(
                userId,
                scoreChange,
                reason,
                LocalDateTime.now()
        );
        reputationLedgerRepository.save(ledger);
    }
}
