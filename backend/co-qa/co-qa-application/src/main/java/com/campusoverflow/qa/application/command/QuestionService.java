package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TagRepository tagRepository;
    private final IntegrationEventPublisher publisher;
    private final Clock clock;

    public QuestionService(QuestionRepository questionRepository,
                           TagRepository tagRepository,
                           IntegrationEventPublisher publisher,
                           Clock clock) {
        this.questionRepository = questionRepository;
        this.tagRepository = tagRepository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostQuestionCommand cmd) {
        Instant now = clock.instant();
        long authorId = actor.userId();

        Title title = new Title(cmd.title());
        MarkdownBody body = MarkdownBody.ofQuestion(cmd.body());
        Tags tags = Tags.of(cmd.tags());

        Question question = Question.post(authorId, cmd.courseId(), title, body, tags, now);
        Question saved = questionRepository.save(question);

        List<String> tagNames = cmd.tags();
        if (tagNames != null && !tagNames.isEmpty()) {
            tagRepository.adjustUsage(tagNames, 1);
        }

        QuestionPostedEvent evt = new QuestionPostedEvent(
                EventIds.next(),
                now,
                saved.id(),
                saved.authorId(),
                saved.courseId(),
                saved.title().value(),
                saved.body().value(),
                tagNames
        );
        publisher.publish(evt);

        return saved.id();
    }
}