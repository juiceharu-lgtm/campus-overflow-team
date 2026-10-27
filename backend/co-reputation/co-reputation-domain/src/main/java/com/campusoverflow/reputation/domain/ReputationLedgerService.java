package com.campusoverflow.reputation.domain;

import java.time.Instant;

public interface ReputationLedgerService {
    boolean existsAccount(Long userId);
    ReputationAccount createAccount(Long userId, int initScore, Instant now);
    void addScore(Long userId, int delta, Instant now);
}
