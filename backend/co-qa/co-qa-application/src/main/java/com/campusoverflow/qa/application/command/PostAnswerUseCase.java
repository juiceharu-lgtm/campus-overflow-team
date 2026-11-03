package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.qa.application.port.QuestionListCache;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostAnswerUseCase {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final IntegrationEventPublisher events;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;

    public PostAnswerUseCase(AnswerRepository answerRepository,
                             QuestionRepository questionRepository,
                             IntegrationEventPublisher events,
                             QuestionListCache listCache,
                             AuditTrail audit,
                             Clock clock) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.events = events;
        this.listCache = listCache;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Long execute(Actor actor, long questionId, MarkdownBody body) {
        Instant now = clock.instant();
        Question question = load(questionId);
        question.ensureOpenForAnswers();

        Answer answer = Answer.submit(question, actor.userId(), body, now);
        Answer saved = answerRepository.save(answer);

        questionRepository.adjustAnswerCount(questionId, 1);

        audit.record("answer.submit", actor.userId(), "Answer", saved.id().toString(), "提交回答");

        AnswerSubmittedEvent evt = new AnswerSubmittedEvent(
                EventIds.next(),
                now,
                saved.id(),
                questionId,
                actor.userId(),
                question.authorId(),
                question.title().value()
        );
        events.publish(evt);
        listCache.invalidateAll();

        return saved.id();
    }

    private Question load(long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("QA‑4001", ""));
    }
}
