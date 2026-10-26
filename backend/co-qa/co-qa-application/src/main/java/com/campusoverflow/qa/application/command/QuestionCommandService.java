package com.campusoverflow.qa.application.command;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** 用例：发布 / 编辑 / 删除问题。 */
@Service
public class QuestionCommandService {
    static final int PLAIN_TEXT_FOR_INDEX = 2000;
    private final QuestionRepository questions;
    private final TagRepository tags;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final ContentRenderer renderer;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;
    public QuestionCommandService(QuestionRepository questions, TagRepository tags, IdentityApi identity,
                                  IntegrationEventPublisher events, ContentRenderer renderer,
                                  QuestionListCache listCache, AuditTrail audit, Clock clock) {
        this.questions = questions;
        this.tags = tags;
        this.identity = identity;
        this.events = events;
        this.renderer = renderer;
        this.listCache = listCache;
        this.audit = audit;
        this.clock = clock;
    }
    @Transactional
    public long post(Actor actor, PostQuestionCommand cmd) {
        Instant now = clock.instant();
        Title title = new Title(cmd.title());
        MarkdownBody body = new MarkdownBody(cmd.body());
        Tags tagsVal = Tags.of(cmd.tags());
        Question question = Question.post(actor.userId(), title, body, tagsVal, now);
        Question saved = questions.save(question);
        audit.record(actor, "question.post", saved.id(), saved);
        String rendered = renderer.renderPlainText(saved.body(), PLAIN_TEXT_FOR_INDEX);
        events.publish(new QuestionPostedEvent(
                EventIds.next(),
                saved.id(),
                actor.userId(),
                saved.title().value(),
                rendered,
                saved.tags().values(),
                now
        ));
        listCache.invalidate();
        return saved.id();
    }
    @Transactional
    public void edit(Actor actor, long questionId, EditQuestionCommand cmd) {
        Instant now = clock.instant();
        Question question = load(questionId);
        Title newTitle = new Title(cmd.title());
        MarkdownBody newBody = new MarkdownBody(cmd.body());
        Tags newTags = Tags.of(cmd.tags());
        question.edit(actor, newTitle, newBody, newTags, now);
        Question saved = questions.save(question);
        audit.record(actor, "question.edit", saved.id(), saved);
        String rendered = renderer.renderPlainText(saved.body(), PLAIN_TEXT_FOR_INDEX);
        events.publish(new QuestionEditedEvent(
                EventIds.next(),
                saved.id(),
                actor.userId(),
                saved.title().value(),
                rendered,
                saved.tags().values(),
                now
        ));
        listCache.invalidate();
    }
    @Transactional
    public void delete(Actor actor, long questionId) {
        Instant now = clock.instant();
        Question question = load(questionId);
        question.softDelete(actor, now);
        Question saved = questions.save(question);
        audit.record(actor, "question.delete", saved.id(), saved);
        events.publish(new ContentDeletedEvent(
                EventIds.next(),
                "question",
                saved.id(),
                actor.userId(),
                now
        ));
        listCache.invalidate();
    }
    private Question load(long questionId) {
        return questions.findById(questionId)
                .orElseThrow(() -> new NotFoundException("问题不存在，id=" + questionId));
    }
}
