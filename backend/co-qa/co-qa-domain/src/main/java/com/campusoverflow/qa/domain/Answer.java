package com.campusoverflow.qa.domain;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import java.time.Instant;
import java.util.Objects;
/**
 * 回答聚合根。独立于 Question 聚合以降低并发冲突，通过 questionId 引用问题。
 * “是否被采纳”由 Question.acceptedAnswerId 决定（单一事实来源），这里不重复存储。
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
    private Answer(Long id, long questionId, long authorId, MarkdownBody body, int score, Long endorsedBy,
                   Instant createdAt, Instant updatedAt, Instant deletedAt, long version) {
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
        // 先保证问题没有被删除，才允许提交回答
        question.ensureOpenForAnswers();
        return new Answer(
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
    }
    public static Answer reconstitute(Long id, long questionId, long authorId, MarkdownBody body, int score,
                                      Long endorsedBy, Instant createdAt, Instant updatedAt, Instant deletedAt,
                                      long version) {
        return new Answer(id, questionId, authorId, body, score, endorsedBy, createdAt, updatedAt, deletedAt, version);
    }
    public void edit(Actor actor, boolean actorManagesCourse, MarkdownBody newBody, Instant now) {
        ensureNotDeleted();
        boolean isAuthor = actor.userId() == this.authorId;
        if (!isAuthor && !actorManagesCourse) {
            throw new ForbiddenException("QA‑3004", "无权编辑该回答");
        }
        this.body = newBody;
        this.updatedAt = now;
    }
    /** 教师认证：课程教师/助教为优质回答打上“教师认证”标记。 */
    public void endorse(Actor actor, boolean actorManagesCourse) {
        ensureNotDeleted();
        if (!actorManagesCourse) {
            throw new ForbiddenException("QA‑3005", "只有课程管理者可以认证回答");
        }
        if (this.endorsedBy != null) {
            throw new ConflictException("QA‑2003", "该回答已经被认证过");
        }
        this.endorsedBy = actor.userId();
    }
    /** 被采纳的回答不能由回答者本人删除（避免“先骗采纳再删除”），课程管理者可删除。 */
    public void softDelete(Actor actor, boolean actorManagesCourse, boolean accepted, Instant now) {
        ensureNotDeleted();
        boolean isAuthor = actor.userId() == this.authorId;
        if (actorManagesCourse) {
            this.deletedAt = now;
        } else if (isAuthor) {
            if (accepted) {
                throw new BusinessRuleException("QA‑1015", "已经被采纳的回答，作者不能删除");
            }
            this.deletedAt = now;
        } else {
            throw new ForbiddenException("QA‑3006", "无权删除该回答");
        }
    }
    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new BusinessRuleException("QA‑1021", "回答已删除，不能执行该操作");
        }
    }
    public boolean isDeleted() {
        return deletedAt != null;
    }
    public void assignId(long id) {
        if (this.id != null) {
            throw new IllegalStateException("ID 已分配");
        }
        this.id = id;
    }
    public Long id() { return id; }
    public long questionId() { return questionId; }
    public long authorId() { return authorId; }
    public MarkdownBody body() { return body; }
    public int score() { return score; }
    public Long endorsedBy() { return endorsedBy; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant deletedAt() { return deletedAt; }
    public long version() { return version; }
}
