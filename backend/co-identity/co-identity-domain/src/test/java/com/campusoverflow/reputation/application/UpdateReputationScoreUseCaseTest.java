package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.domain.ReputationActionType;
import com.campusoverflow.reputation.domain.ReputationScore;
import com.campusoverflow.reputation.domain.ReputationScoreRepository;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateReputationScoreUseCaseTest {

    @Mock
    private ReputationScoreRepository repository;

    @InjectMocks
    private UpdateReputationScoreUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    // Actor record完整构造器：long userId, displayName, Role
    private final Actor actor = new Actor(userId.getLeastSignificantBits(), "test-user", Role.STUDENT);
    private final ReputationActionType actionType = ReputationActionType.QUESTION_PUBLISHED;

    /**
     * 场景：用户不存在，自动初始化声誉并保存
     */
    @Test
    void execute_NewUser_InitScoreAndSave() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        useCase.execute(actor, userId, 5, actionType);

        verify(repository).findByUserId(userId);
        verify(repository).save(any(ReputationScore.class));
    }

    /**
     * 场景：用户已有声誉记录，正常更新并保存
     */
    @Test
    void execute_ExistScore_UpdateAndSave() {
        ReputationScore existed = ReputationScore.create(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existed));

        useCase.execute(actor, userId, -2, actionType);

        verify(repository).findByUserId(userId);
        verify(repository).save(existed);
    }

    /**
     * 场景：传入actor为null，抛出业务异常 REP‑1002
     */
    @Test
    void execute_ActorNull_ThrowException() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(null, userId, 3, actionType));
        assertTrue(ex.getMessage().contains("操作人不能为空"));
    }

    /**
     * 场景：变更后声誉小于0，拒绝本次操作，抛出业务异常 REP‑1001
     * 遵循FF‑8：拒绝整次变更，不修改聚合对象，保障对账不变量
     */
    @Test
    void execute_ScoreLessThanZero_ThrowException() {
        ReputationScore existed = ReputationScore.create(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existed));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(actor, userId, -20, actionType));
        assertTrue(ex.getMessage().contains("声誉分值不能小于0"));
    }
}
