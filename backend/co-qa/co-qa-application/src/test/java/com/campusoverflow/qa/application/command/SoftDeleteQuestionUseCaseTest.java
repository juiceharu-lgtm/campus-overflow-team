package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
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
class SoftDeleteQuestionUseCaseTest {

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
    private SoftDeleteQuestionUseCase useCase;

    private final Actor author = new Actor(1001L, "提问学生", Role.STUDENT);
    private final Actor teacher = new Actor(2001L, "课程教师", Role.TEACHER);
    private final Actor stranger = new Actor(9999L, "无关用户", Role.STUDENT);
    private final Instant fixedNow = Instant.parse("2026-11-01T12:00:00Z");

    @Test
    void success_authorDelete_noAnswer() {
        when(clock.instant()).thenReturn(fixedNow);
        Title title = new Title("软件工程如何学习领域驱动设计");
        MarkdownBody body = new MarkdownBody("这是问题的详细正文内容，描述DDD学习中的疑问，满足长度校验规则");
        Tags tags = Tags.of(List.of("Java"));
        Question q = Question.post(author.userId(), 10L, title, body, tags, fixedNow);
        q.assignId(1L);

        // 只有成功流程才会走到save，这里加桩
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));

        useCase.execute(author, 1L, false);
        verify(questionRepository).save(q);
        verify(audit).record(any(), any(), any(), any(), any());
        verify(events).publish(any());
        verify(listCache).invalidateAll();
    }

    @Test
    void success_teacherDelete_hasAnswer() {
        when(clock.instant()).thenReturn(fixedNow);
        Title title = new Title("软件工程如何学习领域驱动设计");
        MarkdownBody body = new MarkdownBody("这是问题的详细正文内容，描述DDD学习中的疑问，满足长度校验规则");
        Tags tags = Tags.of(List.of("Java"));
        Question q = Question.reconstitute(
                2L,
                author.userId(),
                10L,
                title,
                body,
                tags,
                null,
                0,
                5,
                0,
                fixedNow,
                fixedNow,
                null,
                0
        );
        // 只有成功流程才会走到save，这里加桩
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));
        when(questionRepository.findById(2L)).thenReturn(Optional.of(q));

        useCase.execute(teacher, 2L, true);
        verify(questionRepository).save(q);
        verify(audit).record(any(), any(), any(), any(), any());
        verify(events).publish(any());
        verify(listCache).invalidateAll();
    }

    @Test
    void fail_authorDelete_hasAnswer() {
        when(clock.instant()).thenReturn(fixedNow);
        Title title = new Title("软件工程如何学习领域驱动设计");
        MarkdownBody body = new MarkdownBody("这是问题的详细正文内容，描述DDD学习中的疑问，满足长度校验规则");
        Tags tags = Tags.of(List.of("Java"));
        Question q = Question.reconstitute(
                3L,
                author.userId(),
                10L,
                title,
                body,
                tags,
                null,
                0,
                2,
                0,
                fixedNow,
                fixedNow,
                null,
                0
        );
        // 这里删掉 save 的when！这个用例在q.softDelete就抛异常，不会执行save
        when(questionRepository.findById(3L)).thenReturn(Optional.of(q));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(author, 3L, false));
        assertEquals("QA‑1013", ex.code());
    }

    @Test
    void fail_strangerNoPermission() {
        when(clock.instant()).thenReturn(fixedNow);
        Title title = new Title("软件工程如何学习领域驱动设计");
        MarkdownBody body = new MarkdownBody("这是问题的详细正文内容，描述DDD学习中的疑问，满足长度校验规则");
        Tags tags = Tags.of(List.of("Java"));
        Question q = Question.post(author.userId(), 10L, title, body, tags, fixedNow);
        q.assignId(4L);
        // 删掉 save 的when
        when(questionRepository.findById(4L)).thenReturn(Optional.of(q));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> useCase.execute(stranger, 4L, false));
        assertEquals("QA‑3003", ex.code());
    }

    @Test
    void fail_notFound() {
        when(clock.instant()).thenReturn(fixedNow);
        // 删掉 save 的when，这个用例findById直接返回空，load抛异常，完全不会走到save
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> useCase.execute(author, 9999L, false));
        assertEquals("QA‑4001", ex.code());
    }
}
