package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteQuestionUseCase {

    private final QuestionRepository questionRepository;
    private final IntegrationEventPublisher events;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;

    public SoftDeleteQuestionUseCase(QuestionRepository questionRepository,
                                     IntegrationEventPublisher events,
                                     QuestionListCache listCache,
                                     AuditTrail audit,
                                     Clock clock) {
        this.questionRepository = questionRepository;
        this.events = events;
        this.listCache = listCache;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public void execute(Actor actor, long questionId, boolean isCourseManager) {
        Instant now = clock.instant();
        Question q = load(questionId);
        q.softDelete(actor, isCourseManager, now);
        Question saved = questionRepository.save(q);

        audit.record("question.delete", actor.userId(), "Question", saved.id().toString(), "软删除问题");
        ContentDeletedEvent evt = new ContentDeletedEvent(
                EventIds.next(),
                now,
                "QUESTION",
                saved.id(),
                saved.id()
        );
        events.publish(evt);
        listCache.invalidateAll();
    }

    private Question load(long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("QA‑4001", ""));
    }
}
