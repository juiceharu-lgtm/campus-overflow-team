package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeService;
import com.campusoverflow.reputation.domain.ReputationLedgerService;
import com.campusoverflow.reputation.domain.ReputationRulebook;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@SuppressWarnings("unused")
@Service
public class ReputationEventHandlers {

    private final ReputationLedgerService ledgerService;
    private final BadgeService badgeService;
    private final BountyService bountyService;
    private final Clock clock;

    public ReputationEventHandlers(ReputationLedgerService ledgerService,
                                   BadgeService badgeService,
                                   BountyService bountyService,
                                   Clock clock) {
        this.ledgerService = ledgerService;
        this.badgeService = badgeService;
        this.bountyService = bountyService;
        this.clock = clock;
    }

    public void on(UserRegisteredEvent e) {
        Instant now = clock.instant();
        if (!ledgerService.existsAccount(e.userId())) {
            ledgerService.createAccount(e.userId(), ReputationRulebook.REGISTER_INIT, now);
        }
        badgeService.evaluateAndAward(e.userId(), BadgeAward.Trigger.REGISTER, now);
    }

    public void on(AnswerSubmittedEvent e) {
        Instant now = clock.instant();
        ledgerService.addScore(e.authorId(), ReputationRulebook.ANSWER_SUBMIT, now);
        badgeService.evaluateAndAward(e.authorId(), BadgeAward.Trigger.ANSWER_SUBMIT, now);
    }

    public void on(AnswerAcceptedEvent e) {
        Instant now = clock.instant();
        int delta = ReputationRulebook.acceptedDelta(e.answerAuthorId(), e.questionOwnerId());
        ledgerService.addScore(e.answerAuthorId(), delta, now);

        if (e.bountyId() > 0) {
            bountyService.settleOnAccept(
                    e.questionOwnerId(),
                    e.answerId(),
                    e.answerAuthorId(),
                    e.eventId()
            );
        }
        badgeService.evaluateAndAward(e.answerAuthorId(), BadgeAward.Trigger.ANSWER_ACCEPTED, now);
    }

    public void on(VoteCastEvent e) {
        Instant now = clock.instant();
        // 等VoteCastEvent补上 int value 再打开计分
        // int delta = ReputationRulebook.voteEffect(e.value());
        // ledgerService.addScore(e.targetAuthorId(), delta, now);
        // badgeService.evaluateAndAward(e.targetAuthorId(), BadgeAward.Trigger.VOTE, now);
    }

    public void on(AnswerEndorsedEvent e) {
        Instant now = clock.instant();
        ledgerService.addScore(e.answerAuthorId(), ReputationRulebook.ENDORSE, now);
        badgeService.evaluateAndAward(e.answerAuthorId(), BadgeAward.Trigger.ANSWER_ENDORSED, now);
    }

    @KafkaListener(topics = "${kafka.topics.user-registered}")
    public void listenUserRegistered(UserRegisteredEvent event) {
        on(event);
    }

    @KafkaListener(topics = "${kafka.topics.answer-submitted}")
    public void listenAnswerSubmitted(AnswerSubmittedEvent event) {
        on(event);
    }

    @KafkaListener(topics = "${kafka.topics.answer-accepted}")
    public void listenAnswerAccepted(AnswerAcceptedEvent event) {
        on(event);
    }

    @KafkaListener(topics = "${kafka.topics.vote-cast}")
    public void listenVoteCast(VoteCastEvent event) {
        on(event);
    }

    @KafkaListener(topics = "${kafka.topics.answer-endorsed}")
    public void listenAnswerEndorsed(AnswerEndorsedEvent event) {
        on(event);
    }
}
