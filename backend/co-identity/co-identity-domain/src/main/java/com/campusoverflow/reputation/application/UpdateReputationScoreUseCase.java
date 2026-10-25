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
        // 权限校验：操作者不能为空
        if (actor == null) {
            throw new BusinessRuleException("REP-1002", "操作人不能为空");
        }
        // 查询，无记录则工厂创建（初始10分）
        ReputationScore score = repository.findByUserId(userId)
                .orElseGet(() -> ReputationScore.create(userId));

        // 领域层执行校验，如果会变成负数，这里直接抛异常，后续save不会执行
        score.adjustScore(delta);
        repository.save(score);
    }
}
