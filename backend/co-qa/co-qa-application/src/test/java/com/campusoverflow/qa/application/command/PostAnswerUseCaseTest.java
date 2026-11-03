package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import com.campusoverflow.qa.application.port.QuestionListCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostAnswerUseCaseTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private IntegrationEventPublisher events;

    @Mock
    private QuestionListCache listCache;

    @Mock
    private AuditTrail audit;

    @Mock
    private Clock clock;

    @InjectMocks
    private PostAnswerUseCase useCase;

    private final Actor student = new Actor(1001L, "学生同学", Role.STUDENT);
    private final Instant fixedNow = Instant.parse("2026-11-01T12:00:00Z");

    @Test
    void success_postAnswer() {
        when(clock.instant()).thenReturn(fixedNow);
        Title title = new Title("软件工程如何学习领域驱动设计");
        MarkdownBody qBody = new MarkdownBody("问题正文，长度符合校验，描述我在DDD学习中的困惑");
        Tags tags = Tags.of(List.of("Java"));
        Question question = Question.post(2001L, 10L, title, qBody, tags, fixedNow);
        question.assignId(1L);

        MarkdownBody answerBody = new MarkdownBody("这是回答内容，回答正文满足长度校验，给出学习DDD的实践思路");
        Answer answer = Answer.submit(question, student.userId(), answerBody, fixedNow);
        answer.assignId(100L);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.save(any(Answer.class))).thenReturn(answer);

        Long answerId = useCase.execute(student, 1L, answerBody);
        assertEquals(100L, answerId);
        verify(answerRepository).save(any(Answer.class));
        verify(audit).record(any(), any(), any(), any(), any());
        verify(events).publish(any());
        verify(listCache).invalidateAll();
    }

    @Test
    void fail_questionNotFound() {
        when(clock.instant()).thenReturn(fixedNow);
        MarkdownBody body = new MarkdownBody("这是回答内容，回答正文满足长度校验");
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> useCase.execute(student, 999L, body));
        assertEquals("QA‑4001", ex.code());
    }

    @Test
    void fail_questionAlreadyDeleted() {
        when(clock.instant()).thenReturn(fixedNow);
        Title title = new Title("软件工程如何学习领域驱动设计");
        MarkdownBody qBody = new MarkdownBody("这是问题的详细正文内容，满足长度校验规则");
        Tags tags = Tags.of(List.of("Java"));
        Question deletedQ = Question.reconstitute(
                5L,2001L,10L,title,qBody,tags,null,0,0,0,
                fixedNow,fixedNow,fixedNow,0
        );
        MarkdownBody answerBody = new MarkdownBody("这是回答内容，回答正文满足长度校验");
        when(questionRepository.findById(5L)).thenReturn(Optional.of(deletedQ));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(student,5L,answerBody));
        assertEquals("QA‑1020", ex.code());
    }
}
