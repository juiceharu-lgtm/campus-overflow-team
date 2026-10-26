package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AnswerService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final IntegrationEventPublisher publisher;
    private final Clock clock;

    public AnswerService(QuestionRepository questionRepository,
                         AnswerRepository answerRepository,
                         IntegrationEventPublisher publisher,
                         Clock clock) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostAnswerCommand cmd) {
        Instant now = clock.instant();
        long answerAuthorId = actor.userId();

        Question question = questionRepository.findById(cmd.questionId())
                .orElseThrow(() -> new BusinessRuleException("QA-QUESTION-NOT-FOUND", "问题不存在"));

        MarkdownBody body = MarkdownBody.ofAnswer(cmd.body());
        Answer answer = Answer.submit(question, answerAuthorId, body, now);

        Answer saved = answerRepository.save(answer);

        AnswerSubmittedEvent evt = new AnswerSubmittedEvent(
                EventIds.next(),
                now,
                saved.id(),
                saved.questionId(),
                saved.authorId(),
                question.authorId(),
                question.title().value()
        );
        publisher.publish(evt);

        return saved.id();
    }
}