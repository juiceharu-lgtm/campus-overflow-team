package com.campusoverflow.reputation.application.service;

public interface ReputationLedgerService {
    void addLedger(Long userId, Integer scoreChange, String reason);
}
