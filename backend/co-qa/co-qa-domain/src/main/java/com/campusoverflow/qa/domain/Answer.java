package com.campusoverflow.qa.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public void assignId(long id) {
        if (this.id != null) {
            throw new ConflictException("Answer ID 已经赋值，不能重复 assignId", "QA-2004");
        }
        this.id = id;
    }

    public void edit(Actor actor, boolean actorManagesCourse, MarkdownBody newBody) {
        edit(actor, actorManagesCourse, newBody, Instant.now());
    }

    public void edit(Actor actor, boolean actorManagesCourse, MarkdownBody newBody, Instant now) {
        ensureNotDeleted();
        // 不变式：仅作者本人 / 课程管理者有权编辑
        if (!(this.authorId == actor.userId() || actorManagesCourse)) {
            throw new ForbiddenException("无权编辑", "QA-3004");
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
        // 不变式：只有课程管理者（教师）可以执行 endorse
        if (!actorManagesCourse) {
            throw new ForbiddenException("无权执行教师认证", "QA-3005");
        }
        // 不变式：不能重复认证
        if (this.endorsedBy != null) {
            throw new ConflictException("该回答已经被教师认证过，不可重复认证", "QA-2003");
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
            // 作者自己删：如果回答已经被采纳，禁止删除 QA-1015
            if (answerIsAccepted) {
                throw new BusinessRuleException("被采纳的回答，作者不能删除", "QA-1015");
            }
        } else if (!actorManagesCourse) {
            // 既不是作者，也不是课程管理员：无权删除 QA-3006
            throw new ForbiddenException("无权删除", "QA-3006");
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
            throw new BusinessRuleException("资源已删除，禁止操作", "QA-1021");
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
