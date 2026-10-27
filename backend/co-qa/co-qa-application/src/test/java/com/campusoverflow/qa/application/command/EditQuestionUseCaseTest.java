package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditQuestionUseCaseTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private EditQuestionUseCase useCase;

    private final Actor author = new Actor(1001L, "提问学生", Role.STUDENT);
    private final Actor teacher = new Actor(2001L, "授课老师", Role.TEACHER);
    private final Actor stranger = new Actor(9999L, "无关用户", Role.STUDENT);

    @Test
    void execute_success_authorEdit() {
        Title oldTitle = new Title("旧标题");
        MarkdownBody oldBody = new MarkdownBody("旧内容");
        Tags tags = Tags.of(List.of("Java"));
        Instant now = Instant.now();
        Question q = Question.post(author.userId(), 10L, oldTitle, oldBody, tags, now);
        q.assignId(1L);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));

        Title newTitle = new Title("修改后的标题");
        MarkdownBody newBody = new MarkdownBody("修改后的正文");
        Tags newTags = Tags.of(List.of("Java", "DDD"));

        useCase.execute(author, 1L, false, newTitle, newBody, newTags);
        verify(questionRepository).save(q);
    }

    @Test
    void execute_success_teacherManageEdit() {
        Title oldTitle = new Title("旧标题");
        MarkdownBody oldBody = new MarkdownBody("旧内容");
        Tags tags = Tags.of(List.of("Java"));
        Instant now = Instant.now();
        Question q = Question.post(author.userId(), 10L, oldTitle, oldBody, tags, now);
        q.assignId(1L);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));

        Title newTitle = new Title("教师修改标题");
        MarkdownBody newBody = new MarkdownBody("教师修改正文");
        Tags newTags = Tags.of(List.of("Java", "Spring"));

        useCase.execute(teacher, 1L, true, newTitle, newBody, newTags);
        verify(questionRepository).save(q);
    }

    @Test
    void execute_fail_strangerNoPermission() {
        Title oldTitle = new Title("旧标题");
        MarkdownBody oldBody = new MarkdownBody("旧内容");
        Tags tags = Tags.of(List.of("Java"));
        Instant now = Instant.now();
        Question q = Question.post(author.userId(), 10L, oldTitle, oldBody, tags, now);
        q.assignId(1L);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));

        Title newTitle = new Title("随便改");
        MarkdownBody newBody = new MarkdownBody("乱改内容");
        Tags newTags = Tags.of(List.of("Test"));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> useCase.execute(stranger, 1L, false, newTitle, newBody, newTags));
        assertEquals("QA‑3002", ex.getMessage());
    }

    @Test
    void execute_fail_notFound() {
        Title newTitle = new Title("标题");
        MarkdownBody newBody = new MarkdownBody("内容");
        Tags newTags = Tags.of(List.of("Java"));
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        com.campusoverflow.shared.domain.BusinessRuleException ex = assertThrows(
                com.campusoverflow.shared.domain.BusinessRuleException.class,
                () -> useCase.execute(author, 999L, false, newTitle, newBody, newTags));
        assertEquals("QA-1020", ex.getMessage());
    }
}