package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.domain.BadgePolicy;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.reputation.domain.ReputationRulebook;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅上游上下文的集成事件（由 OutboxRelay 在事务提交后投递，至少一次语义）。
 * 每个处理器运行在自己的事务中，并通过账本唯一键 / 徽章存在性检查实现幂等。
 */
@Component
public class ReputationEventHandlers {

    private final ReputationAccountRepository accounts;
    private final LedgerRepository ledger;
    private final ReputationLedgerService ledgerService;
    private final BadgeService badges;
    private final BountyService bounties;
    private final Clock clock;

    public ReputationEventHandlers(ReputationAccountRepository accounts, LedgerRepository ledger,
                                   ReputationLedgerService ledgerService, BadgeService badges,
                                   BountyService bounties, Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.ledgerService = ledgerService;
        this.badges = badges;
        this.bounties = bounties;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void on(UserRegisteredEvent e) {
        // TODO 后续补齐业务逻辑
//        String ledgerKey = "REGISTER:" + e.userId();
//        if (ledgerService.exists(ledgerKey)) {
//            return;
//        }
//        ReputationAccount account = ReputationAccount.open(e.userId());
//        accounts.save(account);
//        ledgerService.record(ledgerKey, e.userId(), ReputationReason.USER_REGISTER, 0, nowDate());
    }

    /**
     * 场景 R-2 的异步后半段：采纳 → 回答者 +15 → 徽章 → 悬赏结算。
     */
    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        // TODO 后续补齐业务逻辑
//        String ledgerKey = "ACCEPT:" + e.answerId();
//        if (ledgerService.exists(ledgerKey)) {
//            return;
//        }
//        Long authorId = e.answerAuthorId();
//        ReputationAccount account = accounts.findById(authorId)
//                .orElseGet(() -> ReputationAccount.open(authorId));
//
//        account.apply(ReputationRulebook.onAnswerAccepted());
//        accounts.save(account);
//        ledgerService.record(ledgerKey, authorId, ReputationReason.ANSWER_ACCEPTED, 15, nowDate());
//
//        BadgePolicy.evaluate(account).forEach(badge -> badges.grantIfNotExist(account.userId(), badge));
//
//        bounties.settleWhenAccepted(e.bountyId(), e.answerId(), authorId);
    }

    @EventListener
    @Transactional
    public void on(VoteCastEvent e) {
        // TODO 后续补齐业务逻辑
//        String ledgerKey = "VOTE:" + e.voteId();
//        if (ledgerService.exists(ledgerKey)) {
//            return;
//        }
//        Long targetUserId = e.targetAuthorId();
//        ReputationAccount account = accounts.findById(targetUserId)
//                .orElseGet(() -> ReputationAccount.open(targetUserId));
//
//        if (ReputationRulebook.canVoteAddReputation(account, nowDate())) {
//            int delta = ReputationRulebook.onVote(e.voteTargetType());
//            account.apply(delta);
//            accounts.save(account);
//            ledgerService.record(ledgerKey, targetUserId, ReputationReason.VOTE_CAST, delta, nowDate());
//            BadgePolicy.evaluate(account).forEach(badge -> badges.grantIfNotExist(account.userId(), badge));
//        }
    }

    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        // TODO 后续补齐业务逻辑
//        String ledgerKey = "SUBMIT_ANSWER:" + e.answerId();
//        if (ledgerService.exists(ledgerKey)) {
//            return;
//        }
//        Long authorId = e.authorId();
//        ReputationAccount account = accounts.findById(authorId)
//                .orElseGet(() -> ReputationAccount.open(authorId));
//
//        int delta = ReputationRulebook.onAnswerSubmit();
//        account.apply(delta);
//        accounts.save(account);
//        ledgerService.record(ledgerKey, authorId, ReputationReason.ANSWER_SUBMITTED, delta, nowDate());
//        BadgePolicy.evaluate(account).forEach(badge -> badges.grantIfNotExist(account.userId(), badge));
    }

    @EventListener
    @Transactional
    public void on(AnswerEndorsedEvent e) {
        // TODO 后续补齐业务逻辑
//        String ledgerKey = "ENDORSE:" + e.answerId();
//        if (ledgerService.exists(ledgerKey)) {
//            return;
//        }
//        Long authorId = e.answerAuthorId();
//        ReputationAccount account = accounts.findById(authorId)
//                .orElseGet(() -> ReputationAccount.open(authorId));
//
//        int delta = ReputationRulebook.onAnswerEndorsed();
//        account.apply(delta);
//        ledgerService.record(ledgerKey, authorId, ReputationReason.ANSWER_ENDORSED, delta, nowDate());
//        BadgePolicy.evaluate(account).forEach(badge -> badges.grantIfNotExist(account.userId(), badge));
    }

    // 修复Clock报错
    private LocalDate nowDate() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
