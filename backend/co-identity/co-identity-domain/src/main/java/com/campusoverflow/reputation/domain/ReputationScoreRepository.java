package com.campusoverflow.reputation.domain;

import java.util.Optional;
import java.util.UUID;

public interface ReputationScoreRepository {
    Optional<ReputationScore> findByUserId(UUID userId);
    void save(ReputationScore reputationScore);
}
