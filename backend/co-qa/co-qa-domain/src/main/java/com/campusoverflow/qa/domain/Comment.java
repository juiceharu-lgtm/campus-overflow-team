package com.campusoverflow.qa.domain;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.Guard;
import java.time.Instant;
/** 评论：挂在问题或回答下，支持一层“二级回复”。 */
public class Comment {
    private Long id;
    private final TargetType targetType;
    private final long targetId;
    private final long questionId;
    private final long authorId;
    private final Long parentId;
    private final String body;
    private final Instant createdAt;

    private Comment(Long id, TargetType targetType, long targetId, long questionId, long authorId, Long parentId,
                    String body, Instant createdAt) {
        this.id = id;
        this.targetType = targetType;
        this.targetId = targetId;
        this.questionId = questionId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.body = body;
        this.createdAt = createdAt;
    }

    public static Comment post(TargetType targetType, long targetId, long questionId, long authorId, Comment parent,
                               String body, Instant now) {
        // 替换原来 Guard.notNull，使用项目提供的三参数 requireNonNull
        Guard.requireNonNull(body, "QA-1002", "评论内容");
        // 评论正文 2‑600字符，改用 Guard.requireText 统一校验（和项目规范对齐）
        body = Guard.requireText(body, 2, 600, "QA-1003", "评论内容");

        Long realParentId = null;
        if(parent != null){
            // QA‑1016：只允许一层回复，父评论不能再有parent（parent.parentId != null）
            if(parent.parentId() != null){
                throw new BusinessRuleException("QA‑1016","不允许对回复再做回复，只支持一层评论回复");
            }
            // QA‑1017：回复必须和父评论的targetType、targetId完全一致
            if(!parent.targetType().equals(targetType) || parent.targetId() != targetId){
                throw new BusinessRuleException("QA‑1017","回复的目标必须与父评论目标保持一致");
            }
            realParentId = parent.id();
        }
        return new Comment(
                null,
                targetType,
                targetId,
                questionId,
                authorId,
                realParentId,
                body,
                now
        );
    }

    public static Comment reconstitute(Long id, TargetType targetType, long targetId, long questionId, long authorId,
                                       Long parentId, String body, Instant createdAt) {
        return new Comment(id, targetType, targetId, questionId, authorId, parentId, body, createdAt);
    }

    public void assignId(long id) {
        if (this.id != null) {
            throw new IllegalStateException("ID 已分配");
        }
        this.id = id;
    }

    public Long id() { return id; }
    public TargetType targetType() { return targetType; }
    public long targetId() { return targetId; }
    public long questionId() { return questionId; }
    public long authorId() { return authorId; }
    public Long parentId() { return parentId; }
    public String body() { return body; }
    public Instant createdAt() { return createdAt; }
}