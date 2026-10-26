package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoftDeleteQuestionUseCaseTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private SoftDeleteQuestionUseCase useCase;

    private final Actor author = new Actor(1001L, "提问学生", Role.STUDENT);
    private final Actor teacher = new Actor(2001L, "课程教师", Role.TEACHER);
    private final Actor stranger = new Actor(9999L, "无关用户", Role.STUDENT);

    @Test
    void success_authorDelete_noAnswer() {
        Title title = new Title("测试问题");
        MarkdownBody body = new MarkdownBody("内容");
        Tags tags = Tags.of(List.of("Java"));
        Instant now = Instant.now();
        Question q = Question.post(author.userId(), 10L, title, body, tags, now);
        q.assignId(1L);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));
        useCase.execute(author, 1L, false);
        verify(questionRepository).save(q);
    }

    @Test
    void success_teacherDelete_hasAnswer() {
        Title title = new Title("带回答的问题");
        MarkdownBody body = new MarkdownBody("内容");
        Tags tags = Tags.of(List.of("Java"));
        Instant now = Instant.now();
        // 用 reconstitute，直接设置 answerCount=5
        Question q = Question.reconstitute(
                2L,
                author.userId(),
                10L,
                title,
                body,
                tags,
                null,
                0,
                5,  // answerCount = 5（已有回答）
                0,
                now,
                now,
                null,
                0
        );

        when(questionRepository.findById(2L)).thenReturn(Optional.of(q));
        useCase.execute(teacher, 2L, true);
        verify(questionRepository).save(q);
    }

    @Test
    void fail_authorDelete_hasAnswer() {
        Title title = new Title("已有回答");
        MarkdownBody body = new MarkdownBody("内容");
        Tags tags = Tags.of(List.of("Java"));
        Instant now = Instant.now();
        // reconstitute：answerCount=2
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
                now,
                now,
                null,
                0
        );

        when(questionRepository.findById(3L)).thenReturn(Optional.of(q));
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(author, 3L, false));
        assertEquals("QA‑1013", ex.getMessage());
    }

    @Test
    void fail_strangerNoPermission() {
        Title title = new Title("别人的问题");
        MarkdownBody body = new MarkdownBody("内容");
        Tags tags = Tags.of(List.of("Java"));
        Instant now = Instant.now();
        Question q = Question.post(author.userId(), 10L, title, body, tags, now);
        q.assignId(4L);

        when(questionRepository.findById(4L)).thenReturn(Optional.of(q));
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> useCase.execute(stranger, 4L, false));
        assertEquals("QA‑3003", ex.getMessage());
    }

    @Test
    void fail_notFound() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(author, 9999L, false));
        assertEquals("QA-1020", ex.getMessage());
    }
}