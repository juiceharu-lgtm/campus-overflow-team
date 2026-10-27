package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Vote;
import com.campusoverflow.qa.domain.VoteRepository;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用例：投票 / 改投 / 撤销（value ∈ {-1, 0, 1}，0 表示撤销）。
 * 发布的事件同时携带旧值与新值，下游据此计算声誉增量。
 */
@Service
public class VoteService {
    private final VoteRepository votes;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public VoteService(VoteRepository votes, QuestionRepository questions, AnswerRepository answers,
                       IntegrationEventPublisher events, Clock clock) {
        this.votes = votes;
        this.questions = questions;
        this.answers = answers;
        this.events = events;
        this.clock = clock;
    }

    public record VoteResult(int myVote, int scoreDelta) {
    }

    private record Target(long authorId, long questionId) {
    }

    @Transactional
    public VoteResult cast(Actor actor, TargetType type, long targetId, int value) {
        Instant now = clock.instant();
        Target target = resolve(type, targetId);
        long voterId = actor.userId();
        Optional<Vote> existingOpt = votes.find(voterId, type, targetId);

        int oldValue = existingOpt.map(Vote::value).orElse(0);
        int newValue = value;

        // 幂等：新旧值完全一样，什么都不做
        if (oldValue == newValue) {
            return new VoteResult(oldValue, 0);
        }

        int scoreDelta;
        if (newValue == 0) {
            // 撤销投票：删除旧投票
            Vote oldVote = existingOpt.orElseThrow();
            votes.delete(oldVote);
            scoreDelta = 0 - oldValue;
        } else {
            if (existingOpt.isPresent()) {
                // 改投
                Vote vote = existingOpt.get();
                vote.change(newValue, now);
                votes.save(vote);
                scoreDelta = newValue - oldValue;
            } else {
                // 新建投票
                Vote vote = Vote.cast(voterId, target.authorId(), type, targetId, newValue, now);
                votes.save(vote);
                scoreDelta = newValue - oldValue;
            }
        }

        // 原子更新分数
        if (type == TargetType.QUESTION) {
            questions.adjustScore(targetId, scoreDelta);
        } else if (type == TargetType.ANSWER) {
            answers.adjustScore(targetId, scoreDelta);
        }

        // 发布事件，供Reputation消费计算声誉
        VoteCastEvent evt = new VoteCastEvent(
                EventIds.next(),
                now,
                type.name(),
                targetId,
                target.authorId(),
                voterId,
                target.questionId(),
                oldValue,
                newValue
        );
        events.publish(evt);

        int retVote = newValue == 0 ? 0 : newValue;
        return new VoteResult(retVote, scoreDelta);
    }

    private Target resolve(TargetType type, long targetId) {
        if (type == TargetType.QUESTION) {
            Question q = questions.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("QA‑4001", "问题不存在 id=" + targetId));
            return new Target(q.authorId(), q.id());
        } else if (type == TargetType.ANSWER) {
            Answer a = answers.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("QA‑4002", "回答不存在 id=" + targetId));
            return new Target(a.authorId(), a.questionId());
        }
        throw new IllegalArgumentException("不支持的TargetType：" + type);
    }
}