package com.campusoverflow.qa.application.command;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** 用例：提交 / 编辑 / 删除回答，采纳最佳答案，教师认证。 */
@Service
public class AnswerCommandService {
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IdentityApi identity;
    private final AuditTrail audit;
    private final Clock clock;

    public AnswerCommandService(QuestionRepository questions,
                                AnswerRepository answers,
                                IdentityApi identity,
                                AuditTrail audit,
                                Clock clock) {
        this.questions = questions;
        this.answers = answers;
        this.identity = identity;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public long submit(Actor actor, long questionId, String body) {
        Instant now = clock.instant();
        Question question = loadQuestion(questionId);
        MarkdownBody markdownBody = new MarkdownBody(body);
        Answer answer = Answer.submit(question, actor.userId(), markdownBody, now);
        Answer saved = answers.save(answer);
        questions.adjustAnswerCount(questionId, 1);
        audit.record("answer.submit", actor.userId(), "Answer", String.valueOf(saved.id()), "提交回答");
        return saved.id();
    }

    @Transactional
    public void edit(Actor actor, long answerId, String body) {
        Instant now = clock.instant();
        Answer answer = loadAnswer(answerId);
        Question question = loadQuestion(answer.questionId());
        boolean actorManagesCourse = identity.canManageCourse(actor.userId(), question.courseId());
        MarkdownBody newBody = new MarkdownBody(body);
        answer.edit(actor, actorManagesCourse, newBody, now);
        Answer saved = answers.save(answer);
        audit.record("answer.edit", actor.userId(), "Answer", String.valueOf(saved.id()), "编辑回答");
    }

    @Transactional
    public void delete(Actor actor, long answerId) {
        Instant now = clock.instant();
        Answer answer = loadAnswer(answerId);
        Question question = loadQuestion(answer.questionId());
        boolean accepted = question.isAccepted(answerId);
        boolean actorManagesCourse = identity.canManageCourse(actor.userId(), question.courseId());
        answer.softDelete(actor, actorManagesCourse, accepted, now);
        Answer saved = answers.save(answer);
        questions.adjustAnswerCount(answer.questionId(), -1);
        audit.record("answer.delete", actor.userId(), "Answer", String.valueOf(saved.id()), "删除回答");
    }

    /** 采纳回答 */
    @Transactional
    public void accept(Actor actor, long questionId, long answerId) {
        Question question = loadQuestion(questionId);
        Answer answer = loadAnswer(answerId);
        boolean actorManagesCourse = identity.canManageCourse(actor.userId(), question.courseId());
        question.accept(answer, actor, actorManagesCourse);
        questions.save(question);
        audit.record("question.acceptAnswer", actor.userId(), "Question", String.valueOf(question.id()), "采纳回答");
    }

    @Transactional
    public void endorse(Actor actor, long answerId) {
        Instant now = clock.instant();
        Answer answer = loadAnswer(answerId);
        Question question = loadQuestion(answer.questionId());
        boolean actorManagesCourse = identity.canManageCourse(actor.userId(), question.courseId());
        answer.endorse(actor, actorManagesCourse);
        Answer saved = answers.save(answer);
        audit.record("answer.endorse", actor.userId(), "Answer", String.valueOf(saved.id()), "教师认证回答");
    }

    private Question loadQuestion(long id) {
        return questions.findById(id)
                .orElseThrow(() -> new NotFoundException("QA-001", "问题不存在，id=" + id));
    }

    private Answer loadAnswer(long id) {
        return answers.findById(id)
                .orElseThrow(() -> new NotFoundException("QA-002", "回答不存在，id=" + id));
    }
}
