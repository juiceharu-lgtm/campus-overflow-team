package com.campusoverflow.qa.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 回答聚合根。独立于 Question 聚合以降低并发冲突，通过 questionId 引用问题。
 * 是否被采纳由 Question.acceptedAnswerId 决定（单一事实来源），这里不重复存储。
 */
public class Answer {

    private Long id;
    private final long questionId;
    private final long authorId;
    private MarkdownBody body;
    private final int score;
    private Long endorsedBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private final long version;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Answer(Long id,
                   long questionId,
                   long authorId,
                   MarkdownBody body,
                   int score,
                   Long endorsedBy,
                   Instant createdAt,
                   Instant updatedAt,
                   Instant deletedAt,
                   long version) {
        this.id = id;
        this.questionId = questionId;
        this.authorId = authorId;
        this.body = Objects.requireNonNull(body);
        this.score = score;
        this.endorsedBy = endorsedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    public static Answer submit(Question question, long authorId, MarkdownBody body, Instant now) {
        question.ensureOpenForAnswers();

        Answer answer = new Answer(
                null,
                question.id(),
                authorId,
                body,
                0,
                null,
                now,
                now,
                null,
                0
        );

        answer.addEvent(new AnswerSubmittedDomainEvent(
                answer.id(),
                answer.questionId(),
                answer.authorId(),
                now
        ));

        return answer;
    }

    /**
     * 仓储保存后回填聚合 ID。测试和适配器都会调用。
     */
    public void assignId(long id) {
        if (this.id != null) {
            throw new ConflictException("QA-2004", "Answer ID 已经赋值，不能重复 assignId");
        }
        this.id = id;
    }

    public void edit(Actor actor, boolean actorManagesCourse, MarkdownBody newBody) {
        edit(actor, actorManagesCourse, newBody, Instant.now());
    }

    public void edit(Actor actor, boolean actorManagesCourse, MarkdownBody newBody, Instant now) {
        ensureNotDeleted();

        if (!(this.authorId == actor.userId() || actorManagesCourse)) {
            throw new ForbiddenException("QA-3004", "无权编辑该回答");
        }

        this.body = newBody;
        this.updatedAt = now;

        addEvent(new AnswerEditedDomainEvent(
                this.id,
                this.questionId,
                actor.userId(),
                now
        ));
    }

    public void endorse(Actor actor, boolean actorManagesCourse) {
        endorse(actor, actorManagesCourse, Instant.now());
    }

    public void endorse(Actor actor, boolean actorManagesCourse, Instant now) {
        ensureNotDeleted();

        if (!actorManagesCourse) {
            throw new ForbiddenException("QA-3004", "只有课程管理者可以认证回答");
        }

        if (this.endorsedBy != null) {
            throw new ConflictException("QA-2003", "该回答已经被教师认证");
        }

        this.endorsedBy = actor.userId();
        this.updatedAt = now;

        addEvent(new AnswerEndorsedDomainEvent(
                this.id,
                this.questionId,
                actor.userId(),
                now
        ));
    }

    public void softDelete(Actor actor, boolean actorManagesCourse, boolean answerIsAccepted) {
        softDelete(actor, actorManagesCourse, answerIsAccepted, Instant.now());
    }

    public void softDelete(Actor actor, boolean actorManagesCourse, boolean answerIsAccepted, Instant now) {
        ensureNotDeleted();

        if (this.authorId == actor.userId()) {
            if (answerIsAccepted) {
                throw new BusinessRuleException("QA-1012", "被采纳的回答，作者不能删除");
            }
        } else if (!actorManagesCourse) {
            throw new ForbiddenException("QA-3003", "无权删除该回答");
        }

        this.deletedAt = now;
        this.updatedAt = now;

        addEvent(new AnswerDeletedDomainEvent(
                this.id,
                this.questionId,
                actor.userId(),
                now
        ));
    }

    public void ensureNotDeleted() {
        if (isDeleted()) {
            throw new BusinessRuleException("QA-1020", "内容已删除，不能执行该操作");
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public static Answer reconstitute(Long id,
                                       long questionId,
                                       long authorId,
                                       MarkdownBody body,
                                       int score,
                                       Long endorsedBy,
                                       Instant createdAt,
                                       Instant updatedAt,
                                       Instant deletedAt,
                                       long version) {
        return new Answer(id, questionId, authorId, body, score, endorsedBy,
                createdAt, updatedAt, deletedAt, version);
    }

    protected void addEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> pullEvents() {
        List<DomainEvent> copy = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return copy;
    }

    public Long id() {
        return id;
    }

    public long questionId() {
        return questionId;
    }

    public long authorId() {
        return authorId;
    }

    public MarkdownBody body() {
        return body;
    }

    public int score() {
        return score;
    }

    public Long endorsedBy() {
        return endorsedBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public long version() {
        return version;
    }
}
