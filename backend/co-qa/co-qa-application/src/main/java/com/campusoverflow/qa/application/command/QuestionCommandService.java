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
import com.campusoverflow.qa.domain.TagName;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：发布 / 编辑 / 删除问题。 */
@Service
public class QuestionCommandService {
    static final int PLAIN_TEXT_MAX_LENGTH = 2000;
    private final QuestionRepository questions;
    private final TagRepository tagRepository;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final ContentRenderer renderer;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;

    public QuestionCommandService(QuestionRepository questions,
                                   TagRepository tagRepository,
                                   IdentityApi identity,
                                   IntegrationEventPublisher events,
                                   ContentRenderer renderer,
                                   QuestionListCache listCache,
                                   AuditTrail audit,
                                   Clock clock) {
        this.questions = questions;
        this.tagRepository = tagRepository;
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
        Question question = Question.post(actor.userId(), cmd.courseId(), title, body, tagsVal, now);
        Question saved = questions.save(question);
        audit.record("question.post", actor.userId(), "Question", saved.id().toString(), "发布问题");

        List<String> tagList = saved.tags().values().stream()
                .map(TagName::value)
                .collect(Collectors.toList());
        // 新增标签，计数 +1
        tagRepository.adjustUsage(tagList, 1);

        String plainText = renderer.toPlainText(saved.body().value(), PLAIN_TEXT_MAX_LENGTH);
        QuestionPostedEvent event = new QuestionPostedEvent(
                EventIds.next(),
                now,
                saved.id(),
                actor.userId(),
                cmd.courseId(),
                saved.title().value(),
                plainText,
                tagList
        );
        events.publish(event);
        listCache.invalidateAll();
        return saved.id();
    }

    @Transactional
    public void edit(Actor actor, long questionId, EditQuestionCommand cmd) {
        Instant now = clock.instant();
        Question question = load(questionId);
        // 修改前旧标签
        Set<String> oldTags = question.tags().values().stream()
                .map(TagName::value)
                .collect(Collectors.toSet());

        Title newTitle = new Title(cmd.title());
        MarkdownBody newBody = new MarkdownBody(cmd.body());
        Tags newTagsVal = Tags.of(cmd.tags());
        question.edit(actor, identity.canManageCourse(actor.userId(), question.courseId()), newTitle, newBody, newTagsVal, now);
        Question saved = questions.save(question);
        audit.record("question.edit", actor.userId(), "Question", saved.id().toString(), "编辑问题");

        Set<String> newTags = saved.tags().values().stream()
                .map(TagName::value)
                .collect(Collectors.toSet());

        // 移除的标签：旧有、新没有 → -1
        List<String> removed = oldTags.stream()
                .filter(t -> !newTags.contains(t))
                .collect(Collectors.toList());
        if (!removed.isEmpty()) {
            tagRepository.adjustUsage(removed, -1);
        }
        // 新增标签：新有、旧没有 → +1
        List<String> added = newTags.stream()
                .filter(t -> !oldTags.contains(t))
                .collect(Collectors.toList());
        if (!added.isEmpty()) {
            tagRepository.adjustUsage(added, 1);
        }

        List<String> tagList = List.copyOf(newTags);
        String plainText = renderer.toPlainText(saved.body().value(), PLAIN_TEXT_MAX_LENGTH);
        QuestionEditedEvent event = new QuestionEditedEvent(
                EventIds.next(),
                now,
                saved.id(),
                saved.title().value(),
                plainText,
                tagList
        );
        events.publish(event);
        listCache.invalidateAll();
    }

    @Transactional
    public void delete(Actor actor, long questionId) {
        Instant now = clock.instant();
        Question question = load(questionId);
        // 删除前取出标签
        List<String> tagList = question.tags().values().stream()
                .map(TagName::value)
                .collect(Collectors.toList());

        question.softDelete(actor, identity.canManageCourse(actor.userId(), question.courseId()), now);
        Question saved = questions.save(question);
        audit.record("question.delete", actor.userId(), "Question", saved.id().toString(), "删除问题");

        // 删除问题，标签计数 -1
        tagRepository.adjustUsage(tagList, -1);

        ContentDeletedEvent event = new ContentDeletedEvent(
                EventIds.next(),
                now,
                "QUESTION",
                saved.id(),
                saved.id()
        );
        events.publish(event);
        listCache.invalidateAll();
    }

    private Question load(long questionId) {
        return questions.findById(questionId)
                .orElseThrow(() -> new NotFoundException("QA-001", "问题不存在，id=" + questionId));
    }
}