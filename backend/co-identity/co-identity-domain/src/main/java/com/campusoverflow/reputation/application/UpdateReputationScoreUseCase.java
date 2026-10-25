package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.domain.ReputationActionType;
import com.campusoverflow.reputation.domain.ReputationScore;
import com.campusoverflow.reputation.domain.ReputationScoreRepository;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.security.Actor;
import java.util.UUID;

public class UpdateReputationScoreUseCase {

    private final ReputationScoreRepository repository;

    public UpdateReputationScoreUseCase(ReputationScoreRepository repository) {
        this.repository = repository;
    }

    public void execute(Actor actor, UUID userId, int delta, ReputationActionType actionType) {
        if (actor == null) {
            throw new BusinessRuleException("REP-1002", "操作人不能为空");
        }

        ReputationScore score = repository.findByUserId(userId)
                .orElseGet(() -> ReputationScore.create(userId));

        score.adjustScore(delta);
        repository.save(score);
    }
}
