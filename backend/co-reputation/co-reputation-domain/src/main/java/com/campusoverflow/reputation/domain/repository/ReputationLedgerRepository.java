package com.campusoverflow.reputation.domain.repository;

import com.campusoverflow.reputation.domain.entity.ReputationLedger;

public interface ReputationLedgerRepository {
    ReputationLedger save(ReputationLedger ledger);
}
