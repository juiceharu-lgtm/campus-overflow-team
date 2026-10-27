package com.campusoverflow.reputation.domain.repository;

import com.campusoverflow.reputation.domain.ReputationAccount;

public interface ReputationAccountRepository {
    ReputationAccount findByUserId(Long userId);
    ReputationAccount save(ReputationAccount account);
}
