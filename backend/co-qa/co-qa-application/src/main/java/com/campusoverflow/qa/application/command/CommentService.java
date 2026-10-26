package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.CommentRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：评论与二级回复。评论中的 @昵称 由 Discovery 解析并发送提醒。 */
@Service
public class CommentService {
    private final CommentRepository comments;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public CommentService(CommentRepository comments, QuestionRepository questions, AnswerRepository answers,
                          IntegrationEventPublisher events, Clock clock) {
        this.comments = comments;
        this.questions = questions;
        this.answers = answers;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostCommentCommand cmd) {
        Instant now = clock.instant();
        TargetType targetType = cmd.targetType();
        long targetId = cmd.targetId();
        Long parentId = cmd.parentId();
        String body = cmd.body();
        long authorId = actor.userId();

        // 1. 查询目标，拿到 questionId；目标不存在抛404
        long questionId;
        if (targetType == TargetType.QUESTION) {
            Question q = questions.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("QA‑4001", "问题不存在 id=" + targetId));
            questionId = q.id();
        } else if (targetType == TargetType.ANSWER) {
            Answer a = answers.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("QA‑4002", "回答不存在 id=" + targetId));
            questionId = a.questionId();
        } else {
            throw new IllegalArgumentException("不支持的TargetType：" + targetType);
        }

        // 2. 加载父评论（如果有），传给领域工厂做一层回复校验 QA‑1016 QA‑1017
        Comment parentComment = null;
        if (parentId != null) {
            parentComment = comments.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("QA‑4003", "父评论不存在 id=" + parentId));
        }

        // 3. 调用领域聚合工厂，业务校验全部下沉到 Comment.post
        Comment comment = Comment.post(targetType, targetId, questionId, authorId, parentComment, body, now);

        // 4. 保存评论聚合
        Comment saved = comments.save(comment);

        // 5. 发布集成事件，供Discovery解析@昵称
        CommentPostedEvent evt = new CommentPostedEvent(
                EventIds.next(),
                now,
                saved.id(),
                saved.questionId(),
                saved.authorId(),
                saved.body()
        );
        events.publish(evt);

        return saved.id();
    }
}